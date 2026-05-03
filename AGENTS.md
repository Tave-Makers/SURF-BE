# AGENTS.md

이 파일은 Codex가 이 프로젝트에서 작업할 때 참조하는 설정입니다.

## Role

- **대용량 분석 시 Gemini CLI 활용**: `gemini "프롬프트" -y` (10개+ 파일 분석, 최신 기술 정보)

## Project Overview

- **서비스명**: SURF (Tave Makers 커뮤니티 플랫폼)
- **목적**: 동아리/그룹 회원들의 커뮤니티 활동 및 게이미피케이션 플랫폼
- **Stack**: Java 17, Spring Boot 3.x, MySQL (AWS RDS), Redis

## Architecture

**도메인 중심 계층형 아키텍처**

```
com.tavemakers.surf
├── domain/{domain}/
│   ├── controller/   # API 엔드포인트
│   ├── service/      # 비즈니스 로직 (CRUD별 분리)
│   ├── usecase/      # 복합 비즈니스 로직 (선택)
│   ├── repository/, entity/, dto/{request,response}/, exception/
│   └── event/, mapper/, constants/, facade/, validator/, scheduler/ (선택)
└── global/
    ├── common/  # advice/, aop/, encoder/, entity/, exception/, loader/, response/, s3/
    ├── config/  # Security, Redis, QueryDSL, S3, Firebase, Swagger
    ├── jwt/     # JWT 인증 필터 및 서비스
    ├── logging/ # 이벤트 기반 로깅
    └── util/    # EmailSender, SecurityUtils
```

## Core Domains

| 도메인 | 설명 | 특이 구조 |
|--------|------|-----------|
| `member` | 회원 가입/프로필/탈퇴 | `usecase/`, `validator/`, `util/` |
| `post` | 게시글 CRUD/좋아요/검색 | controller·service 하위 `like/post/search/` 분리, `image/support/`, `mapper/`, `scheduler/`, `event/` |
| `comment` | 댓글/대댓글/좋아요 | `event/` |
| `board` | 게시판/카테고리 관리 | |
| `schedule` | 일정 관리 (캘린더) | |
| `scrap` | 게시글 스크랩 | |
| `letter` | 쪽지 | `facade/`, `event/` |
| `score` | 활동 점수 (게이미피케이션) | `usecase/`, `utils/` |
| `badge` | 배지 시스템 | `usecase/` |
| `notification` | FCM 푸시 알림 | `event/` |
| `home` | 홈 화면 배너/콘텐츠 | |
| `login` | 카카오 OAuth2 로그인 | `auth/`, `kakao/` |
| `activity` | 활동 기록 관리 | controller·service·dto 하위 `activeGeneration/`, `activityRecord/` 분리, `usecase/`, `mapper/`, `constants/` |
| `feedback` | 사용자 피드백 | |
| `reservation` | 예약 관리 | `usecase/`, `task/` |
| `team` | 팀/그룹 관리 | |

## Authentication

- **방식**: JWT + OAuth2 (Kakao) / Access + Refresh Token / Redis 저장
- **관련 코드**: `global/jwt/`, `domain/login/kakao/`

## External Services

| 서비스 | 용도 |
|--------|------|
| AWS S3 | 이미지/파일 업로드 |
| AWS RDS | MySQL DB |
| Redis | 토큰 관리, 캐싱 |
| Firebase FCM | 푸시 알림 |
| Kakao OAuth | 소셜 로그인 |
| JavaMail | 이메일 전송 |

## Tech Stack

- **ORM**: Spring Data JPA + QueryDSL
- **ID 생성**: TSID (hypersistence-utils)
- **설정 관리**: spring-dotenv (`.env`)
- **API 문서**: springdoc-openapi (Swagger `http://localhost:8080/swagger-ui.html`)

## Commands

```bash
./gradlew build / bootRun / test
```

---

## Code Conventions

### 네이밍 규칙

| 구분 | 패턴 | 예시 |
|------|------|------|
| Service | `{Domain}{Action}Service` | `PostCreateService` |
| Usecase | `{Domain}Usecase` | `PostDeleteUsecase` |
| Controller | `{Domain}{Action}Controller` | `PostGetController` |
| DTO (Request) | `{Action}ReqDTO` | `PostCreateReqDTO` |
| DTO (Response) | `{Action}ResDTO` | `PostDetailResDTO` |
| Exception | `{Domain}{Error}Exception` | `PostNotFoundException` |
| Event | `{Domain}{Action}Event` | `CommentCreatedEvent` |

**Action 명칭**: `Create`(POST) / `Get`(GET) / `Patch`(PATCH) / `Delete`(DELETE)

### 서비스 계층 규칙

```
Controller → Usecase → Service → Repository
                 ↓
            타 도메인 GetService (조회만 허용)
```

- 타 도메인 Repository 직접 호출 ❌ → GetService 사용 ✅
- 알림 발송: Service 직접 호출 ❌ → Event 발행 ✅
- `@Transactional`: Usecase에서 관리

### 주석 규칙

```java
/** 한줄 설명 (한글, Javadoc 형식) */
public void createPost(PostCreateReqDTO dto) { ... }
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

---

## Design Patterns

| 패턴 | 설명 | 사용 도메인 |
|------|------|-------------|
| **Usecase** | 복합 비즈니스 로직 조합, `@Transactional` 관리 | member, post, score, badge, activity, reservation |
| **Event** | 도메인 간 느슨한 결합 (`@Async @EventListener`) | comment, letter, notification, post |
| **Facade** | 복합 도메인 로직 단순화 (`Controller → Facade → Service`) | letter |
| **Mapper** | Entity ↔ DTO 변환 분리 | post, activity |
| **Scheduler/Task** | 주기적 작업 처리 | post, reservation |
