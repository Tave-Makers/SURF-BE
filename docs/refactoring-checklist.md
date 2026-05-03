# 리팩토링 체크리스트

> Codex 코드 분석 결과 (2026-04-06)
> 3번, 4번 그룹 결과는 분석 완료 후 추가 예정

---

## 1번 그룹: post, comment, board

### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `comment/service/CommentService.java` | `PostRepository` 직접 주입 → `decreaseCommentCount` 호출 | post 도메인에 댓글 수 증감 전용 Service 만들어 경유 |
| 높음 | `comment/service/CommentMentionService.java` | `MemberRepository` 직접 주입 | `MemberGetService` 경유로 교체 |
| 높음 | `post/service/post/PostCreateService.java`, `PostPatchService.java` | Service가 `ReservationUsecase` 직접 참조 (계층 역전) | post Usecase로 예약 조합 책임 이동 |
| 중간 | board 컨트롤러 전체 | Usecase 계층 없이 Controller → Service 직접 호출 | `BoardCreateUsecase` 등 신설 |
| 중간 | comment 컨트롤러 전체 | Usecase 계층 없이 Controller → Service 직접 호출 | Usecase 진입점 신설 |
| 중간 | post 컨트롤러 일부 | Usecase 적용 불균일 (삭제만 Usecase, 나머지 Service 직접) | 전 엔드포인트 Usecase로 통일 |

### N+1 쿼리 위험

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `comment/service/CommentService.java` | 댓글 목록 루프 안에서 멘션/좋아요 매 건 조회 → 연쇄 N+1 | 배치 조회 또는 fetch join |
| 중간 | `post/service/post/PostListService.java` | board/category/member lazy 접근 N+1 | fetch join / DTO projection |
| 중간 | `post/service/search/PostSearchService.java` | 검색 결과 매핑 시 동일 lazy 접근 N+1 | 검색 전용 projection 쿼리 |

### 코드 중복

| 심각도 | 파일들 | 중복 내용 | 해결 방향 |
|--------|--------|-----------|-----------|
| 중간 | `PostCreateService`, `PostListService`, `PostPatchService` | `resolveCategory` 로직 3중 복제 | 공용 validator/Service로 추출 |
| 중간 | `PostCreateService`, `PostPatchService`, `PostGetService` | 이미지 조립 로직 복제 | `PostImageService` 공용 메서드로 통합 |
| 중간 | `PostDeleteService`, `PostPatchService` | `validateOwnerOrManager` 복제 | 권한 검증 전용 Service/validator로 단일화 |

### 성능 이슈

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `comment/service/CommentLikeService.java` | count/exists 목적으로 엔티티 풀 로드 | ID 기반 직접 쿼리로 교체 |
| 중간 | `post/usecase/PostDeleteUsecase.java` + `PostDeleteService.java` | `getPost()` 이중 호출 | Usecase에서 조회 후 Service로 전달 |
| 중간 | `comment/service/CommentMentionService.java` | 전체 조회 후 메모리에서 `.limit(10)` | Repository 쿼리에 `Pageable` 추가 |
| 낮음 | `board/service/BoardService.java` | `existsById` 후 `deleteById` 2회 쿼리 | 한 번 조회 후 삭제로 단일화 |

### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 중간 | `post/dto/response/PostDetailResDTO.java` | `from()` 대신 `of()` 사용, 나머지는 생성자 직접 호출 → `record + from()` 통일 필요 |
| 낮음 | `board/service/BoardService.java`, `comment/service/CommentService.java` | `{Domain}{Action}Service` 규칙 미준수 |

### 보안
- `validateOwnerOrManager` 패턴 적용 확인됨 → 이상 없음

---

## 2번 그룹: member, login, score, badge

### N+1 / 성능

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `badge/repository/MemberBadgeRepository.java` | 배지 수여 회원 조회 후 `getMember()` + `getTracks()` 행마다 추가 쿼리 | fetch join / EntityGraph |
| 높음 | `score/usecase/ScoreRankingUsecase.java` | 랭킹 루프 안에서 `member.getTracks()` 개별 조회 | 트랙 포함 prefetch |
| 높음 | `score/usecase/ScoreRankingUsecase.java` | 전체 회원/팀 메모리 정렬 후 커팅, DB 페이징 미사용 | 정렬/페이징 DB 위임 |
| 중간 | `member/repository/MemberSearchRepository.java` | `leftJoin`(non-fetch) 후 DTO `from()`에서 `getTracks()` 호출 | fetch join 또는 DTO projection |

### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `login/auth/AuthRefreshController.java`, `AuthController.java`, `LogoutController.java` | 컨트롤러가 `MemberRepository`, `JwtService` 직접 주입해 비즈니스 로직 수행 | 전담 Usecase 분리 |
| 중간 | `badge/service/MemberBadgeAssignService.java`, `MemberBadgeGetService.java` | badge 도메인이 `MemberRepository` 직접 참조 | `MemberGetService` 경유로 교체 |

### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `MemberPatchService`, `CareerPatchService`, `CareerDeleteService`, `BadgeUsecase` 등 | Usecase + 하위 Service 양쪽에 `@Transactional` 중복 선언 | 하위 Service의 `@Transactional` 제거 |

### 코드 중복

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 낮음 | `login/auth/AuthController.java`, `member/usecase/MemberAdminUsecase.java` | 토큰 발급 절차(`deviceId → accessToken → refreshToken`) 중복 | 토큰 발급 전담 컴포넌트 추출 |

### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 낮음 | `MemberSearchResDTO`, `MemberSignupResDTO`, `OnboardingCheckResDTO` | `class + @Builder + of()` 형태 → `record + from()` 미준수 |
| 낮음 | `AuthController`, `AdminMemberController`, `MemberService`, `TrackService` | `{Domain}{Action}Controller/Service` 네이밍 규칙 불일치 |

---

## 3번 그룹: notification, activity, reservation

### reservation

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `reservation/task/PostPublishRunner.java:23-26` | `PostRepository` 직접 주입 | post 도메인 GetService 경유로 변경 |
| 높음 | `reservation/task/PostPublishRunner.java:29-44` | Runner가 예약/게시글 뮤테이션 + 이벤트 발행까지 직접 처리 (Usecase 건너뜀) | 게시 흐름 전체를 `ReservationUsecase`로 이동, Runner는 thin adapter로 축소 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `reservation/usecase/ReservationUsecase.java:26-30` | `reservePost`에 `@Transactional` 없음 | `@Transactional` 추가, 스케줄링은 after-commit으로 분리 |
| 높음 | `reservation/task/PostPublishRunner.java:29-30` | `@Transactional`이 Usecase 아닌 Runner에 선언 | Runner 트랜잭션 제거, 전용 Usecase 메서드에 위임 |
| 높음 | `reservation/usecase/ReservationUsecase.java:48-58` | `updateReservationPost` 롤백 시 스케줄러 잔여 job 남음 | `TransactionSynchronizationManager` 또는 `@TransactionalEventListener(AFTER_COMMIT)` 적용 |

#### 성능 이슈

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `ReservationUsecase.java:49-58` + `ReservationScheduleService.java:21-23` | 예약 수정 시 기존 job 취소 없이 새 job 추가 → 만료 job이 계속 wake-up | `ScheduledFuture` 핸들 보존해 이전 job 명시적 unschedule |

### notification

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 치명 | `notification/event/NotificationEventListener.java:29` | `PostRepository` 직접 주입 | `PostGetService` 경유로 변경 |
| 높음 | `controller/DeviceTokenPostController.java:42` | Usecase 없이 Service 직접 호출 | Usecase 계층 도입 |
| 높음 | `controller/NotificationGetController.java:32` | 동일 | 동일 |
| 높음 | `controller/NotificationPatchController.java:28` | 동일 | 동일 |
| 높음 | `controller/FcmTestController.java:39` | `FcmService` 직접 호출 + 이벤트 없이 즉시 발송 | Event 발행 방식으로 전환 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/DeviceTokenRegisterService.java:17` | `@Transactional` Service 계층에 선언 | Usecase로 이동 |
| 높음 | `service/NotificationCreateService.java:30` | 동일 | 동일 |
| 높음 | `service/NotificationService.java:24` | 동일 | 동일 |
| 높음 | `event/NotificationEventListener.java:34` | 이벤트 리스너에서 직접 트랜잭션 시작 | Usecase 위임으로 변경 |

#### N+1 쿼리 위험

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `usecase/NotificationUsecase.java:44` | 공지 알림 전송 시 회원 수만큼 `existsByIdAndStatusNot()` 개별 호출 | 배치 조회로 변경 |

#### 성능 이슈

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `service/NotificationGetService.java:44` | payload JSON을 알림 1건당 여러 번 역직렬화 | 1회 파싱 후 재사용 |

#### 코드 중복 / 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 낮음 | `event/NotificationEventListener.java:55` | 이벤트 핸들러마다 `createAndSend(..., Map.of(...))` 패턴 반복 |
| 중간 | `dto/NotificationResDTO.java:8` | `static from()` 팩토리 없음 |
| 낮음 | `service/NotificationService.java:18` | 포괄적 네이밍, `{Domain}{Action}Service` 규칙 불일치 |

### activity

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `controller/activeGeneration/ActiveGenerationGetController.java:24` | Controller가 Usecase 건너뛰고 Service 직접 호출 | `ActiveGenerationUsecase` 도입 |
| 높음 | `controller/activeGeneration/ActiveGenerationPutController.java:22` | 동일 — Usecase 계층 누락 | Usecase 계층 추가 |
| 높음 | `service/activeGeneration/ActiveGenerationGetService.java:20` | activity Service가 `MemberRepository` 직접 주입 | `MemberGetService` 경유로 변경 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/activeGeneration/ActiveGenerationGetService.java:22` | `@Transactional(readOnly=true)` Service 계층에 선언 | Usecase로 이동 |
| 높음 | `service/activeGeneration/ActiveGenerationPutService.java:16` | `@Transactional` Service 계층에 선언 | Usecase로 이동 |

#### 순환 의존성 위험

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `entity/enums/ActivityType.java`, `ActivityCategory.java` | enum이 Response DTO 임포트, DTO도 enum 참조 → 상호 참조 | enum/entity에서 DTO 생성 로직 제거, Mapper 계층으로 분리 |
| 중간 | `entity/ActivityRecord.java` | Entity가 Request DTO 참조, Response DTO는 entity 참조 → 양방향 결합 | 동일 — Mapper 계층 도입 |

#### 코드 중복

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `usecase/ActivityRecordUsecase.java:48` | personal 기록 생성 로직이 두 메서드에 중복 | 공통 private 메서드로 추출 |
| 중간 | `usecase/ActivityRecordUsecase.java:98` | 페이지네이션+슬라이스 매핑 중복 | 공통 메서드로 추출 |
| 낮음 | `entity/ActivityRecord.java:69` | `ofPersonal()`과 `ofTeam()` 빌더 설정 거의 동일 | 공통 빌더 메서드로 통합 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 중간 | `dto/activityRecord/request/ActivityPenaltyGroupReqDTO.java` 외 | 서버사이드 응답용 DTO가 request 패키지에 위치 |
| 중간 | `dto/activityRecord/response/ActivityRecordSummaryResDTO.java` | Response DTO가 Request DTO 타입 포함 |
| 낮음 | `ActiveGenerationResDTO`, `ActivityCategoryDetailResDTO` 등 다수 | `of()` 팩토리 사용 → `from()`으로 통일 필요 |

#### 성능 이슈

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `controller/activityRecord/ActivityRecordController.java:49` | `pageSize`/`pageNum` 상한 없음 | `@Max` 검증 추가 |
| 중간 | `dto/activityRecord/request/ActivityRecordReqDTO.java:11` | `memberIdList` 크기 제한 없음 | `@Size` 추가 |
| 중간 | `usecase/ActivityRecordUsecase.java:162` | 팀 단건 조회 시 `List.of(...)`로 감싸 첫 번째 요소 접근 | 단건 조회 메서드 추가 |

---

## 4번 그룹: letter, scrap, team, schedule, home, feedback

### letter

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `controller/LetterCreateController.java:22` | Controller가 `LetterFacade` 직접 호출 | `LetterUsecase` 도입 후 경유 |
| 중간 | `controller/LetterGetController.java:21` | 동일 | 동일 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `facade/LetterFacade.java:32` | Facade 계층에서 `@Transactional` 사용 | `LetterUsecase`로 트랜잭션 경계 이전 |
| 높음 | `facade/LetterFacade.java:94` | Facade 계층에서 읽기 트랜잭션 사용 | 동일 |
| 높음 | `service/LetterCreateService.java:16` | Service 계층에서 쓰기 트랜잭션 선언 | 제거 후 Usecase 트랜잭션으로 흡수 |
| 높음 | `service/LetterGetService.java:18` | Service 계층에서 읽기 트랜잭션 선언 | 동일 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 중간 | `dto/request/LetterCreateReqDTO.java:14` | Request DTO가 mutable class → record로 변환 필요 |
| 중간 | `facade/LetterFacade.java:23` | Facade 네이밍이 `{Domain}Usecase` 규칙 위반 → `LetterUsecase`로 리팩토링 |

---

### scrap

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/ScrapService.java:34` | `PostRepository` 타 도메인 직접 주입 | `PostGetService` 경유로 교체 |
| 중간 | `controller/ScrapCreateController.java:20` 외 3개 | Usecase 없이 Service 직접 호출 | `ScrapUsecase` 도입 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/ScrapService.java:29` | Service 계층 `@Transactional` | `ScrapUsecase`로 경계 이전 |
| 높음 | `service/ScrapGetService.java:19` | `deleteByPostId` 쓰기 동작이 GetService 내부에 존재 + 자체 트랜잭션 | 별도 write 서비스로 분리 |

#### 순환 의존성 위험

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `service/ScrapService.java:33` | 주석에 "PostGetService 순환 의존성 우회용"으로 `PostRepository` 직접 사용 명시 | 도메인 경계 재설계 |

#### N+1 쿼리 위험

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `repository/ScrapRepository.java:19` | `findPostsByMemberId`가 bare `Post` 반환 → `PostResDTO.from`에서 board/category/member lazy-load N+1 | fetch join 또는 DTO Projection |

---

### team

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/TeamService.java:32` | `MemberRepository` 타 도메인 직접 주입 | `MemberGetService` 경유 |
| 높음 | `service/TeamService.java:33` | `TrackRepository` 타 도메인 직접 주입 | track 도메인 소유 서비스 경유 |
| 중간 | `controller/TeamCreateController.java:26` 외 3개 | 전체 컨트롤러가 `TeamService` 직접 호출 | `TeamUsecase` 도입 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `service/TeamService.java:36` | Service에서 트랜잭션 관리 | `TeamUsecase`로 이전 |
| 높음 | `service/TeamGetService.java:14` | GetService에서 읽기 트랜잭션 선언 | 제거 |

#### 성능 이슈

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 낮음 | `service/TeamService.java:132` | 멤버 reconciliation 루프에서 `containsMember`/`removeIf` 반복 → O(n²) | Set/Map으로 선계산해 O(n)으로 개선 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 중간 | `service/TeamService.java:29` | generic 네이밍, `{Domain}{Action}Service` 규칙 불일치 |
| 낮음 | `dto/response/TeamGenerationSectionResDTO.java:6` | `from()` 팩토리 없음 |
| 낮음 | `dto/response/TeamDetailResDTO.java:30` | 중첩 record `MemberCardDTO`에 `from()` 없음 |

---

### schedule

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `controller/ScheduleGetController.java:30,38,52` | `ScheduleUsecase`와 `ScheduleGetService`를 동시에 주입/호출 (계층 위반) | 조회 API도 모두 `ScheduleUsecase`로 위임, 컨트롤러에서 Service 의존성 제거 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `ScheduleGetService.java:22`, `ScheduleCreateService.java:17`, `SchedulePatchService.java:15`, `ScheduleDeleteService.java:17` | `@Transactional` Service 계층에 선언 | `ScheduleUsecase` 메서드로 집중 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 낮음 | `dto/response/ScheduleResDTO.java:27`, `ScheduleMonthlyResDTO.java:16` | 팩토리 메서드명이 `fromEntity`, `fromEntities`, `of`로 분산 → `from()`으로 통일 |

---

### home

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `controller/HomeController.java:20` 외 6개 전체 | 모든 컨트롤러가 Usecase 없이 Service 직접 호출 | `HomeUsecase`, `HomeBannerUsecase`, `HomeContentUsecase` 도입 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `service/HomeService.java:38`, `HomeBannerService.java:28`, `HomeContentService.java:20` | Service 계층에 `@Transactional` 선언 | Usecase 계층으로 이동 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 낮음 | `HomeService`, `HomeBannerService`, `HomeController` 등 | `{Domain}{Action}Service/Controller` 패턴 미준수 |
| 낮음 | `dto/response/HomeResDTO.java:7` | `record`이지만 `static from()` 팩토리 없음 |

---

### feedback

#### 아키텍처 위반

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 높음 | `controller/FeedbackCreateController.java:23`, `FeedbackGetController.java:25` | Usecase 없이 `FeedbackService` 직접 호출 | `FeedbackUsecase` 도입 |

#### 트랜잭션 문제

| 심각도 | 파일 | 문제 | 해결 방향 |
|--------|------|------|-----------|
| 중간 | `service/FeedbackService.java:19,28` | `@Transactional` Service 계층에 선언 | `FeedbackUsecase`로 이동 |

#### 일관성 위반

| 심각도 | 파일 | 문제 |
|--------|------|------|
| 낮음 | `service/FeedbackService.java:20` | 생성/조회 역할 혼재, `{Domain}{Action}Service` 패턴 불일치 → `FeedbackCreateService`, `FeedbackGetService`로 분리 |

---

## 전체 우선순위 요약

### 즉시 수정 권장 (높음/치명)

| 순위 | 도메인 | 파일 | 문제 |
|------|--------|------|------|
| 1 | notification | `NotificationEventListener.java:29` | `PostRepository` 직접 참조 (치명) |
| 2 | comment | `CommentService.java` | `PostRepository` 직접 참조 |
| 3 | comment | `CommentMentionService.java` | `MemberRepository` 직접 참조 |
| 4 | scrap | `ScrapService.java:34` | `PostRepository` 직접 참조 + 순환 의존성 위험 |
| 5 | team | `TeamService.java:32,33` | `MemberRepository`, `TrackRepository` 직접 참조 |
| 6 | badge | `MemberBadgeAssignService`, `MemberBadgeGetService` | `MemberRepository` 직접 참조 |
| 7 | login | `AuthRefreshController`, `AuthController`, `LogoutController` | 컨트롤러에서 `MemberRepository`, `JwtService` 직접 주입 |
| 8 | post | `PostCreateService`, `PostPatchService` | Service가 `ReservationUsecase` 참조 (계층 역전) |
| 9 | comment | `CommentService#getComments` | 댓글 목록 연쇄 N+1 |
| 10 | score | `ScoreRankingUsecase` | 전체 회원 메모리 정렬/페이징 |
| 11 | reservation | `ReservationUsecase.java:26-30` | `reservePost` `@Transactional` 누락 |
| 12 | reservation | `ReservationUsecase.java:48-58` | 트랜잭션 롤백 시 스케줄러 잔여 job 남음 |
| 13 | activity | `ActiveGenerationGetService.java:20` | `MemberRepository` 직접 참조 |

### 중기 개선 (중간)

- **Usecase 계층 부재** 도메인 신설: `board`, `comment`, `scrap`, `team`, `letter`, `home`, `feedback`, `schedule`
- **N+1 쿼리 개선**: `MemberBadgeRepository`, `ScoreRankingUsecase`, `ScrapRepository`, `NotificationUsecase`
- **`@Transactional` 중복 정리**: `MemberPatchService`, `CareerPatchService`, `BadgeUsecase` 등
- **activity enum↔DTO 양방향 결합** 해소 (Mapper 계층 도입)
- **reservation** 스케줄러 `ScheduledFuture` 핸들 관리로 좀비 job 방지

### 여유 있을 때 (낮음)

- DTO `record + static from()` 패턴 전체 통일 (letter, activity, team, home 등)
- `{Domain}{Action}Service/Controller` 네이밍 규칙 정비 (home, feedback, score 등)
- 토큰 발급 로직 중복 제거 (`AuthController` ↔ `MemberAdminUsecase`)
- `TeamService` 멤버 reconciliation O(n²) → O(n) 개선
