# 분석 서버 수신 및 저장 가이드

## 개요

이 문서는 SURF-BE에서 비동기 전송한 이벤트 로그를 **분석 서버가 어떻게 받아서 DB에 저장하는지** 설명합니다.
[로그 전송 가이드](./log-forwarding.md)의 후속 문서입니다.

### 전체 흐름

```
[SURF-BE 서버]                       [분석 서버]                    [DB]

유저가 게시글 작성
  ↓
@LogEvent("post.create")
  ↓
flush()
  ├→ log.info(JSON) ← 로컬 파일 저장
  └→ 비동기 HTTP POST ──→ POST /v1/events ──→ event_log 테이블 INSERT
     [JSON 배열]           [받아서 저장]
```

- SURF-BE는 **보내기만** 한다
- 분석 서버는 **받아서 DB에 넣기만** 한다
- SURF-BE의 로그가 이미 구조화된 JSON이므로 별도 파싱이 필요 없다

---

## 비동기 전송이란

SURF-BE에서 분석 서버로 로그를 보낼 때 **비동기(Async)** 방식을 사용합니다.
이것이 무엇인지, 왜 이렇게 하는지 설명합니다.

### 동기 방식 — 보내고 응답 올 때까지 기다림

```
유저 요청 → 비즈니스 로직 → 로그 전송 → 응답 대기... → 전송 완료 → 유저 응답
                                      ~~~~~~~~~~~~~~
                                      이 시간만큼 유저가 기다려야 함
```

분석 서버가 느리면 유저 응답도 느려지고, 분석 서버가 죽으면 유저 요청도 실패합니다.

### 비동기 방식 — 보내고 안 기다림

```
유저 요청 → 비즈니스 로직 → 유저 응답 (즉시 반환)
                         ↘ 별도 스레드에서 로그 전송 (알아서 처리)
```

로그 전송을 **다른 스레드에 맡기고 바로 유저에게 응답**합니다.
분석 서버가 느리거나 죽어도 유저는 영향을 받지 않습니다.

### 코드상 차이

```java
// 동기 — forward()가 끝날 때까지 flush()가 블로킹됨
public void forward(List<String> jsonLines) {
    httpClient.send(request, ...);  // 3초 걸리면 3초 대기
}

// 비동기 — @Async를 붙이면 별도 스레드에서 실행, flush()는 바로 반환
@Async("logForwardExecutor")
public void forward(List<String> jsonLines) {
    httpClient.send(request, ...);  // 별도 스레드에서 알아서 처리
}
```

`@Async`가 하는 일: "이 메서드를 지금 이 스레드 말고 **다른 스레드에서 실행해라**".
그래서 `flush()`는 `forward()`를 호출하자마자 끝나고, 유저에게 응답이 갑니다.

---

## 분석 서버에서 받는 데이터

SURF-BE가 보내는 HTTP 요청 형태:

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
  }
]
```

- 항상 **JSON 배열**로 온다 (한 요청에서 이벤트가 여러 개 발생할 수 있음)
- 같은 배열 안의 이벤트는 `request_id`가 동일하다

### 각 필드 설명

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `timestamp` | string (ISO 8601) | 이벤트 발생 시각 | `"2026-04-13T10:23:45.123Z"` |
| `event` | string | 이벤트명 (도메인.액션 형태) | `"post.create"`, `"comment.delete"` |
| `event_type` | string | 이벤트 유형 | `"INFO"` 또는 `"ERROR"` |
| `result` | string | 요청 성공 여부 | `"success"` 또는 `"fail"` |
| `status` | int | HTTP 상태 코드 | `200`, `400`, `500` |
| `request_id` | string (UUID) | 요청 추적 ID | `"550e8400-..."` |
| `user_id` | string (nullable) | 사용자 내부 ID (비로그인이면 null) | `"42"` |
| `actor_role` | string | 사용자 역할 | `"ROLE_USER"`, `"GUEST"`, `"ROLE_ADMIN"` |
| `http_method` | string | HTTP 메서드 | `"GET"`, `"POST"`, `"DELETE"` |
| `path` | string | 요청 경로 | `"/v1/user/posts"` |
| `duration_ms` | long | 응답 소요 시간 (밀리초) | `234` |
| `message` | string (nullable) | 이벤트 설명 메시지 | `"게시글 생성 성공"` |
| `props` | object | 이벤트별 추가 데이터 (가변) | `{"post_id": 1001, "board_id": 5}` |

---

## DB 테이블 설계

### event_log 테이블

```sql
CREATE TABLE event_log (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    timestamp   DATETIME(3)  NOT NULL,        -- 이벤트 발생 시각 (밀리초 정밀도)
    event       VARCHAR(100) NOT NULL,         -- 이벤트명
    event_type  VARCHAR(10)  NOT NULL,         -- INFO / ERROR
    result      VARCHAR(10)  NOT NULL,         -- success / fail
    status      INT          NOT NULL,         -- HTTP 상태 코드
    request_id  VARCHAR(36)  NOT NULL,         -- 요청 추적용 UUID
    user_id     VARCHAR(50)  NULL,             -- 사용자 ID (비로그인이면 NULL)
    actor_role  VARCHAR(30)  NOT NULL,         -- 역할
    http_method VARCHAR(10)  NOT NULL,         -- HTTP 메서드
    path        VARCHAR(255) NOT NULL,         -- 요청 경로
    duration_ms BIGINT       NOT NULL,         -- 응답 소요시간 (ms)
    message     VARCHAR(500) NULL,             -- 이벤트 메시지
    props       JSON         NULL,             -- 이벤트별 추가 데이터

    INDEX idx_event (event),                   -- 이벤트별 조회
    INDEX idx_user_id (user_id),               -- 유저별 행동 추적
    INDEX idx_timestamp (timestamp),           -- 시간 범위 조회
    INDEX idx_request_id (request_id)          -- 요청 단위 추적
);
```

### 왜 이렇게 설계했는가

- **`props`를 JSON 컬럼으로**: 이벤트마다 추가 데이터가 다르다 (게시글은 `post_id`, 댓글은 `comment_id`). 컬럼을 매번 추가하는 대신 JSON으로 유연하게 저장한다.
- **인덱스 4개**: 가장 자주 쓸 조회 패턴에 맞춤 (이벤트별, 유저별, 시간별, 요청별).
- **`user_id`는 nullable**: 비로그인 사용자 (GUEST)의 이벤트도 기록되기 때문.

---

## 수신 API 구현

분석 서버에서 구현해야 할 API입니다. 프레임워크는 자유이며, 여기서는 Spring Boot 예시를 보여줍니다.

### Controller

```java
@RestController
@RequestMapping("/v1/events")
public class EventIngestionController {

    private final EventIngestionService ingestionService;

    public EventIngestionController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Void> ingest(@RequestBody List<Map<String, Object>> events) {
        ingestionService.saveAll(events);
        return ResponseEntity.ok().build();
    }
}
```

### Service

```java
@Service
public class EventIngestionService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public EventIngestionService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveAll(List<Map<String, Object>> events) {
        String sql = """
            INSERT INTO event_log
            (timestamp, event, event_type, result, status,
             request_id, user_id, actor_role, http_method, path,
             duration_ms, message, props)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        for (Map<String, Object> e : events) {
            jdbc.update(sql,
                e.get("timestamp"),
                e.get("event"),
                e.get("event_type"),
                e.get("result"),
                e.get("status"),
                e.get("request_id"),
                e.get("user_id"),
                e.get("actor_role"),
                e.get("http_method"),
                e.get("path"),
                e.get("duration_ms"),
                e.get("message"),
                toJson(e.get("props"))
            );
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
```

**핵심:**
- JSON 배열을 받아서 for문으로 한 행씩 INSERT
- `props`는 `ObjectMapper`로 다시 JSON 문자열로 변환하여 JSON 컬럼에 저장
- `@Transactional`로 묶어서 한 배치가 전부 성공하거나 전부 실패

---

## 분석 쿼리 예시

저장된 데이터로 할 수 있는 분석:

### 이벤트별 발생 횟수 (오늘)

```sql
SELECT event, COUNT(*) as count
FROM event_log
WHERE timestamp >= CURDATE()
GROUP BY event
ORDER BY count DESC;
```

```
| event          | count |
|----------------|-------|
| post.create    | 87    |
| comment.create | 63    |
| scrap.add      | 41    |
| login.succeeded| 35    |
```

### 특정 유저의 행동 추적

```sql
SELECT timestamp, event, path, message
FROM event_log
WHERE user_id = '42'
ORDER BY timestamp DESC
LIMIT 20;
```

```
| timestamp           | event          | path              | message          |
|---------------------|----------------|-------------------|------------------|
| 2026-04-13 10:23:45 | post.create    | /v1/user/posts    | 게시글 생성 성공 |
| 2026-04-13 10:22:10 | scrap.add      | /v1/user/scraps   | 스크랩 추가 성공 |
| 2026-04-13 10:20:05 | login.succeeded| /login/kakao      | 로그인 성공      |
```

### 에러율 높은 API

```sql
SELECT path,
       COUNT(*) as total,
       SUM(result = 'fail') as fails,
       ROUND(SUM(result = 'fail') / COUNT(*) * 100, 1) as error_rate
FROM event_log
GROUP BY path
ORDER BY error_rate DESC;
```

### 평균 응답시간이 느린 API

```sql
SELECT path,
       COUNT(*) as calls,
       ROUND(AVG(duration_ms)) as avg_ms,
       MAX(duration_ms) as max_ms
FROM event_log
GROUP BY path
ORDER BY avg_ms DESC;
```

### 시간대별 트래픽 분포

```sql
SELECT HOUR(timestamp) as hour, COUNT(*) as count
FROM event_log
WHERE timestamp >= CURDATE()
GROUP BY hour
ORDER BY hour;
```

### 특정 이벤트의 props 분석 (JSON 쿼리)

```sql
-- 게시판별 게시글 생성 수
SELECT JSON_EXTRACT(props, '$.board_id') as board_id, COUNT(*) as count
FROM event_log
WHERE event = 'post.create'
GROUP BY board_id
ORDER BY count DESC;
```

---

## 장애 시나리오

| 상황 | SURF-BE 동작 | 분석 서버 동작 | 데이터 유실 |
|------|-------------|---------------|------------|
| 정상 | 비동기 전송 성공 | 받아서 DB 저장 | 없음 |
| 분석 서버 다운 | 재시도 후 포기 | - | 분석 DB에만 유실 (SURF 로컬 파일에는 남음) |
| 분석 DB 장애 | 정상 전송 | INSERT 실패, 500 응답 | 분석 DB에만 유실 |
| SURF-BE 트래픽 폭증 | 전송 큐 초과분 버림 | 정상 수신 | 초과분만 유실 |

**핵심 원칙:** 분석 시스템의 장애가 SURF-BE 서비스에 영향을 주지 않는다.

---

## 관련 문서

- [커스텀 로깅 시스템](./logging.md) — 이벤트 로그가 어떻게 생성되는지
- [로그 전송 구현 가이드](./log-forwarding.md) — SURF-BE에서 어떻게 보내는지
