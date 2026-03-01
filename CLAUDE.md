# CLAUDE.md

이 파일은 Claude Code가 이 프로젝트에서 작업할 때 참조하는 설정입니다.

## Role

- **Use Gemini CLI as an analysis sub-agent for long-context tasks**
  - 대규모 코드베이스 분석: `gemini "프롬프트" -y`
  - 최신 기술 정보 검색: Gemini 웹 검색 활용
  - 대용량 로그/데이터 전처리: Gemini로 요약 후 결과 활용
  - 상세 가이드: `.claude/agents/gemini-sub-agent.md` 참조

## Project Overview

- **서비스명**: SURF (Tave Makers 커뮤니티 플랫폼)
- **목적**: 동아리/그룹 회원들의 커뮤니티 활동 및 게이미피케이션 플랫폼
- **Type**: Spring Boot 백엔드 API 서버
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Database**: MySQL (AWS RDS)
- **Cache**: Redis (토큰 관리, 캐싱)

## Architecture

**도메인 중심 계층형 아키텍처 (Domain-Centric Layered Architecture)**

```
com.tavemakers.surf
├── domain/           # 비즈니스 도메인
│   ├── {domain}/
│   │   ├── controller/   # API 엔드포인트 (HTTP 메서드별 분리)
│   │   ├── service/      # 비즈니스 로직 (CRUD별 분리)
│   │   ├── usecase/      # 복합 비즈니스 로직 조합 (선택)
│   │   ├── repository/   # 데이터 접근
│   │   ├── entity/       # JPA 엔티티
│   │   ├── dto/
│   │   │   ├── request/  # 요청 DTO
│   │   │   └── response/ # 응답 DTO
│   │   ├── exception/    # 도메인 예외
│   │   ├── event/        # 도메인 이벤트 (선택)
│   │   ├── mapper/       # DTO 매핑 (선택)
│   │   ├── constants/    # 도메인 상수 (선택)
│   │   ├── facade/       # 퍼사드 패턴 (선택)
│   │   ├── validator/    # 도메인 검증 (선택)
│   │   └── scheduler/    # 스케줄링 작업 (선택)
│   └── ...
└── global/           # 공통 기능
    ├── common/
    │   ├── advice/       # 전역 예외 처리 (ControllerAdvice)
    │   ├── aop/          # AOP (annotations/, aspect/)
    │   ├── encoder/      # 인코딩 유틸리티
    │   ├── entity/       # 공통 Base 엔티티
    │   ├── exception/    # 전역 예외 클래스
    │   ├── loader/       # 애플리케이션 초기화 로더
    │   ├── response/     # API 응답 래퍼
    │   └── s3/           # S3 파일 관리 (controller/, service/, dto/, exception/)
    ├── config/           # 설정 (Security, Redis, QueryDSL, S3, Firebase, Swagger 등)
    ├── jwt/              # JWT 인증 필터 및 서비스
    ├── logging/          # 이벤트 기반 로깅 (AOP 연동, 웹 요청 로깅)
    └── util/             # 유틸리티 (EmailSender, SecurityUtils)
```

## Core Domains

| 도메인 | 설명 | 특이 구조 |
|--------|------|-----------|
| `member` | 회원 가입, 프로필, 탈퇴 | `usecase/`, `validator/`, `util/` |
| `post` | 게시글 CRUD, 좋아요, 검색 | controller/service 하위 `like/`, `post/`, `search/` 분리, `mapper/`, `scheduler/` |
| `comment` | 댓글, 대댓글, 좋아요 | `event/` |
| `board` | 게시판/카테고리 관리 | |
| `schedule` | 일정 관리 (캘린더) | |
| `scrap` | 게시글 스크랩 | |
| `letter` | 쪽지 | `facade/`, `event/` |
| `score` | 활동 점수 (게이미피케이션) | `usecase/`, `utils/` |
| `badge` | 배지 시스템 | `usecase/` |
| `notification` | FCM 푸시 알림 | `event/` |
| `home` | 홈 화면 배너/콘텐츠 | |
| `login` | 카카오 OAuth2 로그인 | `auth/`, `kakao/` 하위 구조 |
| `activity` | 활동 기록 관리 | `usecase/`, `mapper/`, `constants/` |
| `feedback` | 사용자 피드백 | |
| `reservation` | 예약 게시글 관리 | `usecase/`, `task/` |
| `team` | 팀/그룹 관리 | |

## Authentication

- **방식**: JWT + OAuth2 (Kakao)
- **토큰**: Access Token + Refresh Token
- **저장**: Redis
- **관련 코드**: `global/jwt/`, `domain/login/kakao/`

## External Services

| 서비스 | 용도 |
|--------|------|
| AWS S3 | 이미지/파일 업로드 |
| AWS RDS | MySQL 데이터베이스 |
| Redis | 토큰 관리, 캐싱 |
| Firebase FCM | 푸시 알림 |
| Kakao OAuth | 소셜 로그인 |
| JavaMail | 이메일 전송 |

## Tech Stack

- **ORM**: Spring Data JPA + QueryDSL (동적 쿼리)
- **ID 생성**: TSID (hypersistence-utils)
- **설정 관리**: spring-dotenv (.env 파일)
- **API 문서**: springdoc-openapi (Swagger)
- **테스트**: JUnit 5

## Commands

```bash
# Build & Run
./gradlew build
./gradlew bootRun
./gradlew test

# API 문서
# 실행 후 http://localhost:8080/swagger-ui.html
```

---

## Code Conventions

### 1. 네이밍 규칙

| 구분 | 패턴 | 예시 |
|------|------|------|
| **Service** | `{Domain}{Action}Service` | `PostCreateService`, `MemberGetService` |
| **Usecase** | `{Domain}Usecase` | `PostDeleteUsecase`, `ScheduleUsecase` |
| **Controller** | `{Domain}{Action}Controller` | `PostCreateController`, `PostGetController` |
| **DTO (Request)** | `{Action}ReqDTO` | `PostCreateReqDTO`, `MemberSignupReqDTO` |
| **DTO (Response)** | `{Action}ResDTO` | `PostDetailResDTO`, `MemberSearchResDTO` |
| **Entity** | `{Domain}` | `Post`, `Member`, `Schedule` |
| **Repository** | `{Domain}Repository` | `PostRepository`, `MemberRepository` |
| **Exception** | `{Domain}{Error}Exception` | `PostNotFoundException` |
| **Event** | `{Domain}{Action}Event` | `CommentCreatedEvent`, `PostLikedEvent` |

### 2. Action 명칭 (CRUD 기준)

| 동작 | 명칭 | HTTP |
|------|------|------|
| 생성 | `Create` | POST |
| 조회 | `Get` | GET |
| 수정 | `Patch` | PATCH |
| 삭제 | `Delete` | DELETE |

### 3. 서비스 계층 규칙

```
Controller → Usecase → Service → Repository
                 ↓
            다른 도메인 GetService (조회만)
```

| 규칙 | 설명 |
|------|------|
| **Repository 직접 참조 금지** | 타 도메인 Repository 직접 호출 ❌ → GetService 사용 ✅ |
| **Service 간 호출** | 조회(Get)만 허용, 생성/수정/삭제는 Usecase에서 조합 |
| **알림 발송** | Service에서 직접 호출 ❌ → Event 발행 ✅ |
| **트랜잭션** | Usecase에서 `@Transactional` 관리 |

### 4. 주석 규칙

```java
/** 한줄 설명 */
public void createPost(PostCreateReqDTO dto) { ... }
```

- 모든 public 메서드에 한줄 주석
- 한글 사용
- Javadoc 형식 (`/** */`)

### 5. DTO 규칙

```java
// Request DTO - record 사용
public record PostCreateReqDTO(
    @NotBlank String title,
    @NotBlank String content,
    Long boardId
) {}

// Response DTO - record + 정적 팩토리
public record PostDetailResDTO(
    Long id,
    String title,
    String content
) {
    public static PostDetailResDTO from(Post post) {
        return new PostDetailResDTO(post.getId(), post.getTitle(), post.getContent());
    }
}
```

### 6. API 경로 규칙

| Prefix | 대상 |
|--------|------|
| `/v1/user/` | 일반 사용자 API |
| `/v1/admin/` | 관리자 API |
| `/v1/manager/` | 매니저 API |
| `/login/` | 인증 관련 |
| `/auth/` | 토큰 재발급 |

---

## Design Patterns

### 1. Usecase 패턴
복합 비즈니스 로직을 조합하는 서비스 계층

```java
@Service
@RequiredArgsConstructor
public class PostDeleteUsecase {
    private final PostDeleteService postDeleteService;
    private final CommentDeleteService commentDeleteService;
    private final ScrapGetService scrapGetService;

    @Transactional
    public void deletePost(Long postId) {
        commentDeleteService.deleteAllByPostId(postId);
        scrapGetService.deleteByPostId(postId);
        postDeleteService.deletePost(postId);
    }
}
```

### 2. Event 패턴
도메인 간 느슨한 결합을 위한 이벤트 기반 통신

```java
// 이벤트 발행
eventPublisher.publishEvent(new CommentCreatedEvent(...));

// 이벤트 처리
@Async
@EventListener
public void handleCommentCreated(CommentCreatedEvent event) {
    notificationCreateService.createAndSend(...);
}
```

### 3. Facade 패턴
복합 도메인 로직을 단순화하는 퍼사드 계층 (letter 도메인에서 사용)

```
Controller → Facade → Service → Repository
```

### 4. Mapper 패턴
Entity ↔ DTO 변환 로직 분리 (post, activity 도메인에서 사용)

### 5. Scheduler 패턴
주기적 작업 처리 (post/scheduler, reservation/task에서 사용)

### 6. TSID
시간순 정렬 가능한 분산 ID 생성

### 7. 환경변수 분리
`.env` 파일로 민감 정보 관리
