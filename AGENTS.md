# AGENTS.md

이 파일은 Codex 등 AI 에이전트가 이 프로젝트에서 작업할 때 참조하는 설정입니다.

## Role

- **대용량 분석 시 Gemini CLI 활용**: `gemini "프롬프트" -y` (10개+ 파일 분석, 최신 기술 정보)

## Project Overview

- **서비스명**: SURF (Tave Makers 커뮤니티 플랫폼)
- **목적**: 동아리/그룹 회원들의 커뮤니티 활동 및 게이미피케이션 플랫폼
- **Stack**: Java 17, Spring Boot 3.x, MySQL, Redis

## Architecture

**계층 우선(layer-first) 클린 아키텍처** — 2026-07 전면 리팩토링 완료 (설계 근거: `docs/refactoring-plan.md`)

```
com.tavemakers.surf
├── presentation/{domain}/     # controller/, dto/{request,response}/
├── application/{domain}/      # usecase/, query/, event/, scheduler/, task/
├── domain/{domain}/           # entity/, repository/, service/, exception/, event/
├── infrastructure/{domain}/   # 기술 구체 (QueryDSL·Jdbc 리포지토리, 외부 API 클라이언트, FCM)
└── global/
    ├── common/  # advice/, aop/, encoder/, entity/, exception/, loader/, response/, s3/
    ├── config/  # Security, Redis, QueryDSL, S3, Firebase, Swagger, Scheduler, Async
    ├── jwt/     # JWT 인증 필터 및 서비스
    ├── logging/ # 이벤트 기반 로깅, Server-Timing 헤더
    └── util/    # EmailSender, SecurityUtils
```

- `auth`는 도메인 하위를 provider로 슬라이스: `{apple, kakao, common}`

### 계층 책임 (B안)

| 계층 | 책임 | 금지 |
|------|------|------|
| **presentation** | API 엔드포인트, Req/Res DTO | repository·infrastructure 직접 의존 |
| **application** | usecase·query — **`@Transactional` 소유**, 도메인 서비스 조합, **엔티티→DTO 매핑** | — |
| **domain** | 엔티티, 도메인 서비스(원시값 입력·**엔티티 반환·DTO 무지**), Spring Data 리포지토리 인터페이스 | infrastructure 의존(R8), DTO 참조 |
| **infrastructure** | 손으로 쓴 기술 구체 — QueryDSL/JdbcTemplate 리포지토리, 외부 API 클라이언트 | — |

### 레포지토리 배치 절충안

- **Spring Data JPA 인터페이스** (메서드 시그니처만) → `domain/{domain}/repository/` — 사실상 포트 역할
- **손으로 쓴 구체 클래스** (QueryDSL, JdbcTemplate 등) → `infrastructure/{domain}/` — 예: `MemberSearchRepository`, `PostJdbcRepository`
- 구현체 1개짜리 인터페이스 포트는 만들지 않는다 (불필요한 추상화 금지)

### ArchUnit 의존성 규칙 (자동 검증)

`src/test/java/.../architecture/CleanArchitectureRulesTest.java` — 전체 테스트에 포함되어 위반 시 빌드 실패.

| 규칙 | 내용 |
|------|------|
| R1a~c | controller는 repository·infrastructure 직접 의존 금지, usecase/query만 호출 |
| R2 | 타 도메인의 비(非)Get 서비스 호출 금지 |
| R3 | 타 도메인 repository 직접 참조 금지 |
| R4 | `@Transactional`은 application 계층(usecase/query)에만 |
| R5 | 트랜잭션 안에서 외부 API 호출 금지 |
| R6 | 비동기 부수효과 리스너는 `@TransactionalEventListener(AFTER_COMMIT)` 사용 |
| R8 | domain 계층은 infrastructure 의존 금지 (freeze 없는 즉시 실패) |

일부 규칙은 `FreezingArchRule`로 기존 위반을 동결(`src/test/resources/archunit-store/`). **새 위반은 실패**하며, 부채를 청산했으면 store 파일을 삭제 후 arch 테스트를 1회 돌려 재동결한다.

## Core Domains

| 도메인 | 설명 | 비고 |
|--------|------|------|
| `member` | 회원 가입/프로필/탈퇴 | 탈퇴 3종: withdraw/expel=소프트(익명화+소셜계정 삭제), dismiss=하드 |
| `auth` | 소셜 로그인 (Kakao/Apple) + 토큰 | provider 슬라이스, RTR(refresh 회전) |
| `post` | 게시글 CRUD/좋아요/검색 | controller·dto 하위 `like/post/search/` 분리, 조회수는 `PostJdbcRepository` 배치 반영 |
| `comment` | 댓글/대댓글/좋아요 | 연속 중복 등록 방지(5초 윈도) |
| `board` | 게시판/카테고리 관리 | |
| `schedule` | 일정 관리 (캘린더) | |
| `scrap` | 게시글 스크랩 | |
| `letter` | 쪽지 | |
| `score` | 활동 점수 (게이미피케이션) | |
| `badge` | 배지 시스템 | |
| `notification` | FCM 푸시 알림 | 목록 조회는 Slice 페이지네이션, `member_id` 인덱스 |
| `home` | 홈 화면 배너/콘텐츠 | |
| `activity` | 활동 기록 관리 | dto 하위 `activeGeneration/`, `activityRecord/` 분리 |
| `feedback` | 사용자 피드백 | |
| `reservation` | 게시글 예약 발행 | presentation 없음 (post가 트리거, `task/`로 실행) |
| `team` | 팀/그룹 관리 | |

## Authentication

- **방식**: JWT + OAuth2 (Kakao, Apple) / Access + Refresh Token
- **RTR**: refresh 회전 — Redis Lua CAS로 단일 사용 보장, grace key 멱등 처리, 재사용 탐지 시 전 세션 폐기
- **로그인 식별**: `social_account(provider, provider_id)` 기준. 탈퇴 시 소셜 계정 연결도 삭제(재가입 허용), 퇴출자는 블랙리스트 차단
- **관련 코드**: `global/jwt/`, `domain/auth/`, `application/auth/`

## External Services

| 서비스 | 용도 |
|--------|------|
| AWS S3 | 이미지/파일 업로드 |
| MySQL (RDS) | DB |
| Redis | refresh 토큰(RTR), 조회수 캐싱 |
| Firebase FCM | 푸시 알림 |
| Kakao / Apple OAuth | 소셜 로그인 |
| JavaMail | 이메일 전송 |

## Observability

- **Actuator**: `/actuator/metrics/http.server.requests` — 엔드포인트별 응답시간 (관리자 역할 전용, `/actuator/health`만 무토큰 허용)
- **Server-Timing**: 모든 REST 응답에 `Server-Timing: app;dur=<ms>` 헤더 (`global/logging/ServerTimingResponseAdvice`)
- **이벤트 로깅**: `LogEventEmitter`로 요청 컨텍스트에 적재 → `WebLoggingFilter`가 flush → 분석 서버 전송(옵션)

## Tech Stack

- **ORM**: Spring Data JPA + QueryDSL
- **ID 생성**: TSID (hypersistence-utils)
- **설정 관리**: spring-dotenv (`.env`)
- **API 문서**: springdoc-openapi (Swagger `http://localhost:8080/swagger-ui.html`)

## Commands

```bash
./gradlew build / bootRun / test
./gradlew test --tests "*CleanArchitectureRulesTest"   # 아키텍처 규칙만
```

---

## Code Conventions

### 네이밍 규칙

| 구분 | 패턴 | 예시 |
|------|------|------|
| 도메인 Service | `{Domain}{Action}Service` | `PostCreateService` |
| Usecase | `{Domain}Usecase` | `PostDeleteUsecase` |
| Query 서비스 | `{Domain}GetService` | `MemberGetService` |
| Controller | `{Domain}{Action}Controller` | `PostGetController` |
| DTO (Request) | `{Action}ReqDTO` | `PostCreateReqDTO` |
| DTO (Response) | `{Action}ResDTO` | `PostDetailResDTO` |
| Exception | `{Domain}{Error}Exception` | `PostNotFoundException` |
| Event | `{Domain}{Action}Event` | `CommentCreatedEvent` |

**Action 명칭**: `Create`(POST) / `Get`(GET) / `Patch`(PATCH) / `Delete`(DELETE)

### 서비스 계층 규칙

```
Controller → Usecase/Query(@Transactional, DTO 매핑) → 도메인 Service(엔티티 반환) → Repository
                      ↓
              타 도메인 GetService (조회만 허용)
```

- 타 도메인 Repository 직접 호출 ❌ → GetService 사용 ✅ (R2·R3)
- 알림 발송: Service 직접 호출 ❌ → Event 발행 ✅ (R6: AFTER_COMMIT 리스너)
- `@Transactional`: application 계층에서만 (R4)
- 도메인 서비스는 DTO를 모른다 — 매핑은 usecase/query에서

### 주석 규칙

```java
/** 한줄 설명 (한글, Javadoc 형식) */
public void createPost(...) { ... }
```

### DTO 규칙

```java
// Request - record 사용
public record PostCreateReqDTO(@NotBlank String title, Long boardId) {}

// Response - record + 정적 팩토리
public record PostDetailResDTO(Long id, String title) {
    public static PostDetailResDTO from(Post post) { ... }
}
```

### API 경로 규칙

| Prefix | 대상 |
|--------|------|
| `/v1/user/` | 일반 사용자 |
| `/v1/admin/` | 관리자 |
| `/v1/manager/` | 매니저 |
| `/login/` | 인증 |
| `/auth/` | 토큰 재발급 |
| `/actuator/` | 메트릭·헬스체크 (health 외 관리자 전용) |

---

## Design Patterns

| 패턴 | 설명 | 위치 |
|------|------|------|
| **Usecase** | 도메인 서비스 조합, `@Transactional` 소유, DTO 매핑 | `application/{domain}/usecase/` |
| **Query** | 읽기 전용 조회(read-model 조립 포함) | `application/{domain}/query/` |
| **Event** | 도메인 간 느슨한 결합 (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) | 발행: domain, 리스너: `application/{domain}/event/` |
| **Mapper** | Entity ↔ DTO 변환 분리 | post, activity |
| **Scheduler/Task** | 주기적 작업 (조회수 배치 반영, 예약 발행) | `application/post/scheduler/`, `application/reservation/task/` |
