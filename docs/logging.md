# 커스텀 로깅 시스템

## 개요

SURF-BE는 **이벤트 기반 구조화 로깅 시스템**을 사용합니다.
단순 문자열 로그가 아닌, JSON 형식의 구조화된 이벤트 로그를 요청 단위로 수집하여 출력합니다.

---

## 아키텍처

### 디렉토리 구조

```
global/
├── logging/
│   ├── LogEvent.java            # 메서드 레벨 이벤트 애노테이션
│   ├── LogParam.java            # 파라미터 레벨 애노테이션
│   ├── LogEventContext.java     # ThreadLocal 런타임 커스터마이징
│   ├── LogEventEmitter.java     # 이벤트 발행 인터페이스
│   ├── LogEventEmitterImpl.java # 이벤트 수집 및 JSON 출력
│   ├── LogEventAspect.java      # AOP Aspect
│   ├── WebLoggingFilter.java    # 서블릿 필터 (요청 컨텍스트 관리)
│   └── RequestLogContext.java   # 요청별 ThreadLocal 컨텍스트
└── common/aop/
    ├── annotations/ExecutionTimeLog.java
    └── aspect/ExecutionTimeAspect.java
```

### 요청 흐름

```
HTTP 요청
  ↓
[WebLoggingFilter]
  - RequestLogContext 초기화
  - 요청 ID, HTTP 메서드, 경로 저장
  - 사용자 정보 추출 (ID, 역할)
  ↓
[서비스 메서드 @LogEvent]
  ↓
[LogEventAspect]
  - @LogParam 파라미터 추출
  - 메서드 실행
  - 반환값 ID 자동 추출
  - LogEventContext.drain() → 런타임 커스터마이징 반영
  ↓
[LogEventEmitter.emit()]
  - 이벤트를 RequestLogContext.events 리스트에 저장
  ↓
HTTP 응답
  ↓
[WebLoggingFilter finally 블록]
  - 응답 시간 계산
  - HTTP 상태 코드 저장
  - emitter.flush() → 모든 이벤트를 JSON으로 출력
  - RequestLogContext.clear() → ThreadLocal 정리
```

---

## 이벤트 로깅 (`global/logging/`)

### 핵심 컴포넌트

| 컴포넌트 | 역할 |
|----------|------|
| `@LogEvent` | 메서드에 이벤트명 지정 (예: `"post.like"`) |
| `@LogParam` | 파라미터를 로그 props에 포함 |
| `LogEventContext` | ThreadLocal 기반 런타임 커스터마이징 (`put()`, `overrideEvent()`) |
| `LogEventAspect` | AOP로 `@LogEvent` 메서드를 가로채서 파라미터/반환값 자동 수집 |
| `WebLoggingFilter` | 요청별 컨텍스트 관리 (request_id, userId, 경로, 소요시간) |
| `LogEventEmitterImpl` | 이벤트를 수집 후 flush 시 JSON 출력 |
| `RequestLogContext` | 요청 단위 ThreadLocal 컨텍스트 (요청 정보 + 이벤트 목록) |

### 기본 사용법

```java
@LogEvent(value = "post.like", message = "게시물 좋아요 성공")
public void like(
    @LogParam("post_id") Long postId,
    @LogParam("user_id") Long memberId
) {
    // 비즈니스 로직
}
```

### 런타임 커스터마이징

서비스 메서드 내부에서 동적으로 로그 속성을 추가하거나 이벤트명을 변경할 수 있습니다.

```java
@LogEvent(value = "post.like", message = "좋아요")
public void like(@LogParam("post_id") Long postId) {
    // 동적 속성 추가
    LogEventContext.put("liked", true);
    LogEventContext.put("board_id", boardId);

    // 조건부 이벤트명 변경
    if (post.getBoard().getType() == BoardType.NOTICE) {
        LogEventContext.overrideEvent("notice_like_toggle");
        LogEventContext.overrideMessage("공지 좋아요 버튼 클릭");
    }
}
```

### 실패 이벤트 자동 생성

메서드에서 예외가 발생하면 `{이벤트명}.failed` 이벤트가 자동 생성됩니다.

```java
// 예외 발생 시 자동으로 "post.like.failed" 이벤트 생성
// props에 error_code, error_msg 자동 포함
```

### 반환값 ID 자동 추출

메서드 반환값이 엔티티인 경우, ID를 자동으로 props에 포함합니다.

### 출력 형태

```json
{
  "timestamp": "2026-04-13T10:23:45.123Z",
  "event": "post.like",
  "event_type": "INFO",
  "result": "success",
  "status": 200,
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "user_id": "42",
  "actor_role": "ROLE_USER",
  "http_method": "POST",
  "path": "/api/v1/posts/123/like",
  "duration_ms": 156,
  "message": "게시물 좋아요 성공",
  "props": {
    "post_id": 123,
    "user_id": 42,
    "liked": true
  }
}
```

---

## 실행 시간 로깅 (`global/common/aop/`)

`@ExecutionTimeLog` 애노테이션으로 메서드 실행 시간을 측정합니다.

### 사용법

```java
@ExecutionTimeLog(jobName = "스케줄러 작업")
public void someScheduledJob() {
    // ...
}
// 출력: [스케줄러 작업] 실행 완료 (소요시간: 156ms)
```

---

## 등록된 이벤트 목록

| 도메인 | 이벤트명 예시 |
|--------|--------------|
| 게시글 | `post.create`, `post.update`, `post.delete`, `post.like`, `post.unlike` |
| 회원 | `signup.create`, `signup.approve`, `signup.reject`, `member.profile_update` |
| 로그인 | `login.kakao.request`, `login.kakao.callback`, `login.succeeded`, `login.failed` |
| 댓글 | `comment.create`, `comment.delete`, `comment.like.toggle` |
| 스크랩 | `scrap.add`, `scrap.remove`, `scrap.list.view` |
| 게시판 | `board.create`, `board.update`, `board.delete` |
| 기타 | `feedback.create`, `calendar.view`, `calendar_summary_open` |

---

## 설계 특징

- **구조화된 이벤트**: 자유 문자열이 아닌 정해진 이벤트명 + props로 일관된 로그 포맷
- **요청 단위 추적**: `request_id`로 한 요청의 모든 이벤트를 연결
- **런타임 동적 변경**: `LogEventContext.overrideEvent()`로 조건부 이벤트명 변경 가능
- **자동 ID 추출**: 반환값에서 entity ID를 자동으로 추출하여 props에 포함
- **PII 보호**: 이메일 등 개인정보 대신 내부 ID만 기록
- **비동기 안전성**: ThreadLocal 기반으로 동시성 안전
