# 로그 이벤트 → 분석 서버 전송 구현 가이드

## 개요

현재 `LogEventEmitterImpl.flush()`는 로컬 파일/콘솔에만 JSON을 출력합니다.
이 문서는 **flush 시점에 비동기 HTTP로 분석 서버에 동시 전송**하는 구현 방법을 설명합니다.

### 변경 전후 비교

```
[현재]
flush() → log.info(JSON) → 로컬 파일에만 저장

[변경 후]
flush() → log.info(JSON)            ← 기존 로컬 로그 유지 (안전망)
       → @Async HTTP POST(JSON)    ← 분석 서버로 비동기 전송
```

---

## 변경 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `LogEventEmitterImpl.java` | flush 시 JSON Line 변환 + 전송 서비스 호출 |
| `LogForwardingService.java` | **신규** — 비동기 HTTP 전송 담당 |
| `LogForwardingProperties.java` | **신규** — 전송 설정 (URL, 활성화 여부 등) |
| `AsyncConfig.java` | **신규 또는 기존 수정** — @Async 전용 스레드 풀 설정 |
| `application.yml` | 전송 설정 값 추가 |

---

## 1단계: 설정 클래스

### LogForwardingProperties.java

```java
package com.tavemakers.surf.global.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging.forwarding")
public record LogForwardingProperties(
    boolean enabled,     // 전송 활성화 여부
    String url,          // 분석 서버 엔드포인트 (예: https://analytics.example.com/v1/events)
    int timeoutMs,       // HTTP 타임아웃 (기본 3000ms)
    int maxRetries       // 실패 시 재시도 횟수 (기본 1)
) {
    public LogForwardingProperties {
        if (timeoutMs <= 0) timeoutMs = 3000;
        if (maxRetries < 0) maxRetries = 1;
    }
}
```

### application.yml 추가

```yaml
logging:
  forwarding:
    enabled: false          # local/dev에서는 false, 운영에서 true
    url: ${LOG_FORWARD_URL:}
    timeout-ms: 3000
    max-retries: 1
```

- `.env` 또는 환경변수로 `LOG_FORWARD_URL`을 주입
- 분석 서버가 준비되기 전까지 `enabled: false`로 기존 동작 유지

---

## 2단계: 비동기 전송 서비스

### AsyncConfig.java

```java
package com.tavemakers.surf.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("logForwardExecutor")
    public Executor logForwardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);       // 큐가 차면 버린다 (로그 유실 허용)
        executor.setThreadNamePrefix("log-fwd-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 큐 초과 시 조용히 버림 — 로컬 파일에는 이미 남아있으므로 안전
        });
        executor.initialize();
        return executor;
    }
}
```

**설계 포인트:**
- 전용 스레드 풀을 분리하여 비즈니스 스레드에 영향 없음
- 큐 초과 시 버리는 정책 — 분석 로그가 서비스를 죽이면 안 됨

### LogForwardingService.java

```java
package com.tavemakers.surf.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@ConditionalOnProperty(name = "logging.forwarding.enabled", havingValue = "true")
public class LogForwardingService {

    private static final Logger log = LoggerFactory.getLogger(LogForwardingService.class);

    private final HttpClient httpClient;
    private final LogForwardingProperties props;

    public LogForwardingService(LogForwardingProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.timeoutMs()))
                .build();
    }

    /**
     * JSON Line 배열을 분석 서버로 비동기 전송.
     * flush() 한 번당 한 번 호출되므로 한 요청의 모든 이벤트가 하나의 배치로 전송된다.
     */
    @Async("logForwardExecutor")
    public void forward(List<String> jsonLines) {
        // JSON Array로 묶어서 전송 (벌크)
        String body = "[" + String.join(",", jsonLines) + "]";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.url()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofMillis(props.timeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        for (int attempt = 0; attempt <= props.maxRetries(); attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return; // 성공
                }
                log.warn("[LogForward] 전송 실패 (status={}, attempt={}/{})",
                        response.statusCode(), attempt, props.maxRetries());
            } catch (Exception e) {
                log.warn("[LogForward] 전송 예외 (attempt={}/{}): {}",
                        attempt, props.maxRetries(), e.getMessage());
            }
        }
        // 모든 재시도 실패 — 로컬 파일에는 이미 남아있으므로 여기서 끝
    }
}
```

---

## 3단계: LogEventEmitterImpl 수정

```java
@Component
public class LogEventEmitterImpl implements LogEventEmitter {

    private static final Logger log = LoggerFactory.getLogger("api-event");
    private final ObjectMapper objectMapper;
    private final LogForwardingService forwardingService; // nullable

    public LogEventEmitterImpl(
            ObjectMapper objectMapper,
            @Autowired(required = false) LogForwardingService forwardingService
    ) {
        this.objectMapper = objectMapper;
        this.forwardingService = forwardingService;
    }

    // emit(), emitError()는 기존과 동일 (변경 없음)

    public void flush() {
        RequestLogContext ctx = RequestLogContext.get();
        List<String> jsonLines = new ArrayList<>();   // ← 추가

        for (Map<String, Object> e : ctx.events) {
            Map<String, Object> record = new HashMap<>();
            record.put("timestamp", Instant.now().toString());
            record.put("event", e.get("event"));
            record.put("event_type", e.get("event_type"));
            record.put("result", ctx.status >= 400 ? "fail" : "success");
            record.put("status", ctx.status);
            record.put("request_id", ctx.requestId);
            record.put("user_id", ctx.userId);
            record.put("actor_role", ctx.actorRole);
            record.put("http_method", ctx.httpMethod);
            record.put("path", ctx.path);
            record.put("duration_ms", ctx.durationMs);
            if (e.containsKey("message")) {
                record.put("message", e.get("message"));
            }
            record.put("props", e.getOrDefault("props", Collections.emptyMap()));

            try {
                // 로컬 로그 (기존 유지)
                String json = objectMapper.writeValueAsString(record);  // ← pretty → 한줄로 변경
                log.info(json);
                jsonLines.add(json);                                    // ← 전송용 수집
            } catch (Exception ex) {
                log.warn("Failed to write log record: {}", ex.toString());
            }
        }

        // 분석 서버 전송 (비동기)
        if (forwardingService != null && !jsonLines.isEmpty()) {
            forwardingService.forward(jsonLines);
        }
    }
}
```

**변경 포인트:**
- `writerWithDefaultPrettyPrinter()` → `writeValueAsString()` (JSON Line으로 변경)
- `forwardingService`는 `@Autowired(required = false)` — 비활성화 시 null이므로 기존 동작 그대로

---

## 전송 데이터 형식

분석 서버에 전송되는 HTTP Body 예시:

```http
POST /v1/events HTTP/1.1
Content-Type: application/json

[
  {
    "timestamp": "2026-04-13T10:23:45.123Z",
    "event": "post.create",
    "event_type": "INFO",
    "result": "success",
    "status": 200,
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "user_id": "42",
    "actor_role": "ROLE_USER",
    "http_method": "POST",
    "path": "/v1/user/posts",
    "duration_ms": 234,
    "message": "게시글 생성 성공",
    "props": {
      "post_id": 1001,
      "board_id": 5
    }
  },
  {
    "timestamp": "2026-04-13T10:23:45.125Z",
    "event": "notification.send",
    "event_type": "INFO",
    "result": "success",
    "status": 200,
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "user_id": "42",
    "actor_role": "ROLE_USER",
    "http_method": "POST",
    "path": "/v1/user/posts",
    "duration_ms": 234,
    "message": "알림 전송",
    "props": {}
  }
]
```

- 한 요청에서 발생한 모든 이벤트가 **하나의 배열**로 묶여서 전송
- `request_id`가 동일하므로 분석 서버에서 요청 단위로 묶어 분석 가능

---

## 장애 시나리오별 동작

| 시나리오 | 동작 | 로그 유실 |
|----------|------|----------|
| 분석 서버 정상 | 비동기 전송 성공 | 없음 |
| 분석 서버 다운 | 재시도 후 포기, warn 로그 | 분석 서버에만 유실 (로컬 파일에는 남음) |
| 분석 서버 느림 (타임아웃) | 3초 후 타임아웃, 재시도 | 동일 |
| 전송 큐 초과 (트래픽 폭증) | 초과분 버림 | 동일 |
| `enabled: false` | 전송 안 함 | 해당 없음 (기존 동작) |

**핵심 원칙: 분석 로그 전송 실패가 서비스에 영향을 주면 안 된다.**

---

## 향후 확장

분석 서버 스펙이 확정되면 추가로 고려할 사항:

| 항목 | 설명 |
|------|------|
| **인증** | API Key 헤더, Bearer 토큰 등 → `HttpRequest.header()` 에 추가 |
| **배치 전송** | 이벤트를 메모리에 모아뒀다가 N개 또는 N초마다 한꺼번에 전송 |
| **Kafka 전환** | 트래픽 증가 시 HTTP 대신 Kafka Producer로 교체 |
| **Dead Letter** | 전송 실패 이벤트를 별도 파일에 저장 후 나중에 재전송 |
