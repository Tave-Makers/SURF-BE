# 클린 아키텍처 리팩토링 계획

> 2026-07-06 수립. 도메인별 상세 위반 목록은 [refactoring-checklist.md](refactoring-checklist.md) 참고.
> 현행 아키텍처 설명은 [architecture.md](architecture.md) — 이 문서의 목표 구조가 완성되면 그쪽을 갱신한다.

## 원칙

전면 재작성이 아니라 **본질 3가지만 엄격하게** 가져간다:

1. 의존성은 안쪽으로만 (presentation → application → domain)
2. 도메인 간 결합은 계약(query 조회 / 도메인 이벤트)으로만
3. 규칙은 문서가 아니라 코드(ArchUnit)로 강제

**의도적 트레이드오프**: JPA 엔티티를 도메인 모델에서 분리하지 않는다 (순수 도메인 객체 + 매핑 계층 도입 안 함).
프레임워크 독립성보다 매핑 코드 폭증·학습 비용이 크다고 판단. 같은 이유로 타 도메인 엔티티의
JPA 연관관계 참조(@ManyToOne Member 등)는 허용한다.

## 목표 구조

```
com.tavemakers.surf
├── domain/{domain}/
│   ├── presentation/          # Controller + Request/Response DTO
│   │   ├── controller/
│   │   └── dto/
│   ├── application/           # 유일한 트랜잭션 경계, 오케스트레이션
│   │   ├── usecase/           # 쓰기 (커맨드)
│   │   └── query/             # 읽기 전용 (기존 GetService 승격, 타 도메인에 공개되는 유일한 창구)
│   ├── domain/                # Entity, 도메인 서비스, Repository 인터페이스, 도메인 이벤트, 예외
│   └── infrastructure/        # QueryDSL 구현체, 외부 API 클라이언트 (S3/FCM/Kakao/Apple/Mail)
└── global/                    # 공통 인프라 (jwt, config, logging, 공통 응답)
```

## 의존성 규칙 R1~R6

`src/test/java/com/tavemakers/surf/architecture/CleanArchitectureRulesTest.java`에서 ArchUnit으로 강제한다.

| # | 규칙 |
|---|------|
| R1 | Controller는 자기 도메인의 application(usecase/query)만 호출 |
| R2 | 타 도메인 접근은 상대 도메인의 query(읽기) 또는 도메인 이벤트(쓰기)만 |
| R3 | 타 도메인 Repository 직접 참조 금지 |
| R4 | @Transactional은 application 계층에만 |
| R5 | 트랜잭션 안에서 외부 API(S3/FCM/Kakao/Apple/Mail) 호출 금지 → 커밋 후 이벤트로 |
| R6 | 도메인 간 부수효과는 @TransactionalEventListener(AFTER_COMMIT)로 통일 |

### FreezingArchRule 베이스라인

기존 위반 약 342건(2026-07-06 기준: R1b 75, R2 62, R3 39, R4 154, R5 7, R6 5)은
`src/test/resources/archunit-store/`에 동결되어 있다.

- **새 위반만 테스트를 실패시킨다** — 신규 코드는 규칙을 지켜야 머지 가능
- 기존 위반은 Phase 3 도메인 전환 때 청산하며, 고치면 스토어에서 자동으로 줄어든다
- 주의: 위반 라인이 이동하면(파일 수정) 동결 매칭이 깨져 재동결이 필요할 수 있다.
  그 경우 해당 위반을 고치는 것이 원칙, 불가피하면 테스트 재실행으로 스토어 갱신 후 diff 확인

R5는 정적 근사치다(직접 의존만 탐지). 간접 호출은 PR 리뷰(arch-reviewer)에서 잡는다.

## 실행 단계

| Phase | 내용 | 상태 |
|-------|------|------|
| 0 | ArchUnit R1~R6 + 베이스라인, 동시성 테스트 헬퍼(`support/ConcurrencyTestHelper`), 본 문서 | ✅ |
| 1 | P0 버그 수정 — 확정 4건 수정·커밋, 2건은 검증 결과 오탐 종결 (arch-reviewer APPROVE) | ✅ |
| 2 | badge 도메인 파일럿 전환 → 4계층 템플릿 + 도메인 점검 체크리스트 확립 | ✅ |
| 3 | 도메인별 전환 + 전 로직 점검 — Wave 1~4 완료, 15개 도메인 전부 4계층 전환 (2026-07-09) | ✅ |
| 4 | global 정리, 베이스라인 청산, FreezingArchRule 해제 (아래 로드맵) | 진행 중 |

### Phase 4 — 베이스라인 청산 로드맵 (2026-07-09 수립)

Phase 3 완료 시점 동결 베이스라인 **589건** (R1a·R5·R6은 0 — 완전 청산). FreezingArchRule이
신규 유입을 차단하므로 부채는 더 늘지 않는다. 청산은 위험·규모 순으로 단계화한다:

| 단계 | 대상 | 규모 | 방식 | 상태 |
|------|------|------|------|------|
| 4-1 | R3 (타 도메인 repo 직접 참조) | 6건 | 도메인 서비스 창구 신설(ReservationDeleteService, PostPublishService) — 발행 race 수정 동반 | ✅ 0건 |
| 4-2 | global 정리 | — | jwt import 버그, 예외 메시지 노출, AccessDenied 403, frameOptions, 데드코드 | 진행 중 |
| 4-3 | R4a/R4b (@Transactional 위치) | ~101건 | domain/service의 트랜잭션을 application(usecase/query)으로 승격 — 호출 경로별 트랜잭션 경계 재검토 필요, 도메인 단위 PR | 로드맵 |
| 4-4 | R1b (controller→비Get service 직접 호출) | 72건 | usecase 위임 계층 신설 — R4-3과 같은 PR에서 자연 해소되는 경우 많음 | 로드맵 |
| 4-5 | R2 (타 도메인 비-query 호출) | ~69건 | 이벤트화 또는 커맨드 포트 정식화(PostCommentCountService류) — 정합성 요구별 개별 판단 | 로드맵 |
| 4-6 | R7 (domain→application/presentation 역의존) | ~339건 | 엔티티 팩토리의 ReqDTO 파라미터를 원시값으로, 서비스의 ResDTO 반환을 매퍼 분리로 — **API·생성자 계약 대수술, 팀 리뷰 필수** | 로드맵 |
| 4-7 | R1c (controller→infrastructure) | 3건 | FcmTestController(테스트용) — 운영 필요성 자체를 팀 확인 후 삭제 또는 usecase 경유 | 보류 |
| 4-8 | 0건 도달 규칙부터 FreezingArchRule 해제 | — | R1a/R5/R6/R3는 이미 0 — freeze를 벗겨도 그린(즉시 가능), 나머지는 단계별 | 부분 가능 |

4-3~4-6은 로직 변경(트랜잭션 경계·API 계약)이므로 이 리팩토링 브랜치가 dev에 머지·안정화된 후
도메인 단위 PR로 진행하는 것을 권장한다.

### Phase 1 — P0 버그 결과 (2026-07-07 완료)

1. ✅ 댓글 좋아요 lost update — `likeCount` 원자적 UPDATE + 중복 등록 race는 `CommentLikeAlreadyExistsException`(409)으로 전환
2. ✅ 활동 점수 lost update — 쓰기 경로에 PESSIMISTIC_WRITE `ForUpdate` 조회 도입 (읽기 경로는 무잠금 유지)
3. ✅ 배지 부여 race — `saveAllAndFlush`로 unique 위반을 커밋 전 감지, 도메인 예외로 변환 (트랜잭션은 원래 usecase에 있었음 — 스캔 오탐 정정)
4. ✅ FCM 무효 토큰 비활성화 유실 — `DeviceTokenUsecase`(@Transactional) 벌크 UPDATE로 교체.
   배지 회수/예약 생성/활동기록의 "@Transactional 누락" 주장은 usecase 트랜잭션 존재로 오탐 종결
5. ✅ 알림 읽음 처리 — `findByIdAndMemberId`로 소유권 필터 조회
6. ✖️ 쪽지 메일 예외 삼킴 — 오탐 (이미 에러 로그 + `LetterMailSendFailException` throw 중)

### 설계 결정 (2026-07-07 확정)

**D1. MemberDismissUsecase 해체 — 동기 인-트랜잭션 이벤트 하이브리드 (비동기 이벤트 아님)**
계정 삭제(제명)는 "전부 성공 또는 전부 롤백"이어야 하므로 정합성이 결합도보다 우선한다.
AFTER_COMMIT 비동기 이벤트로 내부 정리를 하면 회원 row는 지워졌는데 리스너 실패 시 고아 데이터가
남아 되돌릴 수 없다 → 채택하지 않는다.
- 내부 독립 정리(badge/notification/deviceToken/score/letter/activity/commentMention):
  `MemberDismissedEvent`를 트랜잭션 안에서 발행하고 각 도메인이 **동기 `@EventListener`**(같은 트랜잭션)로
  자기 데이터를 삭제. 예외 시 전체 롤백. member가 타 도메인을 컴파일 타임에 의존하지 않게 됨.
  (이벤트명 주의: withdraw/expel에서는 발행되지 않으므로 Withdrawn이 아니라 Dismissed)
- 외부 효과: Kakao/Apple 연결 해제는 `MemberDisconnectedEvent`(AFTER_COMMIT `@Async`),
  Redis 최근검색 정리는 `RecentSearchCleanupListener`(AFTER_COMMIT) — 외부·비트랜잭션·best-effort.
- 순서·데이터 의존부: 팀 리더 위임 → team의 `TeamMemberCleanupService`(팀 삭제 부속 정리는
  `TeamDeletedEvent` 동기 리스너), 게시글 일괄 삭제 → `PostDeleteUsecase.deleteAllOwnedBy`
  (deletedPostIds 반환 → 댓글 정리에 전달). Repository 직접 주입 제거 (R3 -33건).
- R6 규칙 정교화: 금지 대상은 `@Async` + plain `@EventListener`(커밋 전 별도 스레드로 새는 부수효과).
  동기 인-트랜잭션 정리 리스너는 허용. **✅ 구현 완료 (889d4f87)**

**D2. 탈퇴/퇴출 회원 연관 데이터 = 현행 정책 유지 (의도된 설계)**
expel(퇴출)·withdraw(자진 탈퇴)는 회원을 익명화("탈퇴한 회원") + soft delete만 하고 게시글/댓글/
좋아요/스크랩/팀소속은 보존한다. 전체 정리는 dismiss(제명)만 수행한다. 이는 "콘텐츠 보존 + 계정 차단"
의도이므로 감사에서 결함으로 재플래그하지 않는다.

### Phase 3 이관 항목 (Phase 1 리뷰에서 도출, non-blocking)

- `PersonalScoreGetService`의 ForUpdate 메서드는 "GetService = 읽기" 계약을 흐림 → score 도메인 command 포트로 이관
- `FcmService → DeviceTokenUsecase` 계층 역행 → 개명 또는 이벤트 분리
- `LetterFacade`: 메일 발송(외부 API)이 @Transactional 안 + 저장 전 발송 순서 (R5, Phase 2 트랜잭션 경계 정리 대상)
- `ReservationUsecase`: 트랜잭션 커밋 전 스케줄 등록 → 롤백 시 좀비 job (Codex 지적과 동일, Phase 2)
- `ActivityRecordUsecase.scoreCalculator` 미사용 필드 (데드코드)
- `removeAllByMemberId` 류 반복 delete → 벌크 전환
- **R2 사각지대 (Wave 1 구조 이동 리뷰에서 발견)**: `*ApiClient`가 infrastructure로 이동하면서 타 도메인의 infrastructure 의존(예: `MemberDisconnectedListener → AppleApiClient`)이 R2 매칭 대상에서 벗어남. Phase 4에서 R2를 `.infrastructure`까지 확장하거나 auth revoke 창구를 이벤트로 정리
- **ScrapGetService.deleteByPostId (Wave 2 scrap 구조 이동에서 발견)**: query(application/query) 계층에 쓰기 메서드 `deleteByPostId`가 `@Transactional`(readOnly=false)로 존재 — "query=읽기" 계약 위반. usecase 또는 domain service로 재배치 필요(동작 동일성이 자명하지 않아 구조 이동에서 미처리)
- **R7 역의존 청산 (Wave 2 구조 이동 리뷰에서 신설)**: R7 규칙 추가 — domain 계층은 같은 도메인 application(usecase/query)·presentation(controller/dto)에 의존 금지. 기존 179건 동결(엔티티가 request DTO 수신: Post.update(PostUpdateReqDTO)/Member/Career 등, domain 서비스→GetService, 일부 repository→DTO 프로젝션). 신규 유입만 차단. Phase 4에서 청산: 엔티티 생성/수정을 원시 값 파라미터로, domain이 필요로 하는 조회를 domain repository로 하강
- **ActivityRecord.prefixSum 스냅샷 미보정 (Wave 3 심사 non-blocking)**: 833c7774이 PersonalActivityScore 누적합은 고쳤으나, ActivityRecord 행마다 저장되는 prefixSum 스냅샷은 patch/delete 후 여전히 갱신 안 됨. 스냅샷 컬럼의 실제 소비처 추적 후 보정 여부 결정 (소비처 없으면 데드 컬럼 정리)
- **ScoreCalculator/ScoreComputable 데드코드**: 구현체 없는 인터페이스 + ActivityRecordUsecase의 미사용 주입 필드 — Phase 4 정리
- **viewCount 후속 (Wave 2 post 버그 리뷰 non-blocking)**: ① `ViewCountService` Redis 장애 폴백(`post.increaseViewCount()` dirty write)을 `@Modifying` 원자 UPDATE로 전환 — increment 이후 예외 시 off-by-one + 스케줄러 합산과 교차 유실 창 제거. ② `ViewCountScheduler` 클래스 `@Transactional` 하에서 배치 여러 개가 한 트랜잭션 → 뒤 배치 JDBC 실패 시 GET+DEL로 이미 회수·삭제된 앞 배치 델타까지 전량 유실. 배치 단위 트랜잭션 분리 또는 재적재 검토

### Phase 3 — 도메인 전환 Wave

| Wave | 도메인 | 비고 |
|------|--------|------|
| 1 | badge(파일럿), member + auth | MemberDismissUsecase 해체(✅ D1), auth·member 4계층 구조 이동(✅ be77c698, 182bf06a — arch-reviewer APPROVE) |
| 2 | post, comment, scrap | 트래픽 핵심, 멱등성 이슈 밀집. scrap↔post 순환 의존 해소 포함 |
| 3 | notification, score, letter | ✅ 완료 — 알림 리스너 5종 AFTER_COMMIT(R6 0건), letter 저장먼저·메일 트랜잭션 밖(R5), score prefixSum 정합성, 구조 이동 3커밋, R1c 신설 |
| 4 | board, schedule, activity, reservation, team, home, feedback, login | 상대적으로 단순 |

### 구조 전환 체크리스트 (badge 파일럿에서 확립)

1. main과 **test 소스 트리를 동일하게** `git mv` (sed로 패키지 선언만 바꾸면 IDE에서 깨짐 — Gradle은 통과하므로 빌드만 믿지 말 것)
2. 패키지/임포트 치환 순서: 구체 클래스(GetService FQCN) → 일반 패키지 순. 와일드카드 import는 명시 import로 분해
3. 구조 이동으로 동결 위반의 FQCN이 바뀌면: `freeze.refreeze=true` 일시 추가 → 테스트 1회 → 플래그 제거 → 재실행 확인. 스토어 diff가 순수 rename인지 커밋 전 확인
4. 위반 라인을 움직인 커밋에서 스토어 갱신을 함께 커밋
5. **query 반환 DTO 소속**: query 서비스는 presentation DTO를 반환해도 된다(허용 트레이드오프 — record 매핑 계층을 별도로 만들지 않는다). 단 도메인 서비스(domain/service)는 DTO를 몰라야 한다

### 도메인 전환 사이클 (도메인당)

```
① domain-auditor(Opus) 심층 스캔 — 관점 분할 2~3개 병렬 (권한·멱등성 / 트랜잭션·이벤트 / 조회·정합성)
② finding-verifier(Fable) 오탐 검증 — 높음 심각도는 2개 교차 판정
③ concurrency-test-writer(Opus) 재현 테스트 → bug-fixer(Fable) 수정
④ layer-migrator(Opus) 4계층 재배치 (로직 변경 금지, badge 템플릿 추종)
⑤ arch-reviewer(Fable) 최종 심사 → PR
```

에이전트 정의: `.claude/agents/` 6종.

### 도메인 점검 체크리스트 (Phase 2에서 확정, 초안)

- [ ] 권한: 모든 쓰기 엔드포인트에 소유자/역할 검증
- [ ] 멱등성: 중복 요청 시 카운트/상태 정합성 (unique 제약 + 예외 처리)
- [ ] 동시성: 카운트·누적 필드는 원자적 UPDATE 또는 락
- [ ] 트랜잭션: application 계층에만, 외부 API는 경계 밖, readOnly 구분
- [ ] 이벤트: AFTER_COMMIT 통일, 리스너 실패가 본 트랜잭션에 영향 없음
- [ ] 조회: N+1, 반복 delete/save → 벌크
- [ ] soft delete 정합성: 탈퇴 회원/삭제 게시글의 연관 데이터 처리
- [ ] 예외: 삼킴 없음 / 시간: 타임존 명시

### Wave 4 감사 발견 — 정책 결정 필요 항목 (2026-07-08, 즉시 수정 불가)

- **board 삭제 FK 500 (높음, 칩 발행)**: `BoardService.deleteBoard`가 하위 카테고리/게시글 검증 없이 deleteById → FK 위반 500. 차단(하위 존재 시 409) vs 캐스케이드 정책 결정 필요
- **schedule 단독 삭제 dangling (중간)**: `ScheduleUsecase.deleteSchedule`이 연동 게시글의 scheduleId/hasSchedule을 초기화하지 않음 (deleteScheduleAtPost와 비대칭). 연동 일정의 단독 삭제 허용 여부 정책
- **board rename 시 Post.boardName stale (중간)**: 비정규화 컬럼 전파 vs 수용 결정
- **schedule category 자유문자열 (중간, 추정)**: "regular"/"other" 하드코딩 필터 vs 자유 입력("정규행사" 예시) — enum화 필요하나 기존 데이터 정본 확인 필요
- **feedback 일 3회 제한 동시성 우회 (중간)**: check-then-act. 제한 구현 방식(락/원자 카운터) 결정
- **타임존 미지정 (낮음, 전역)**: schedule/home/feedback의 LocalDateTime.now()가 서버 기본 존 의존 — JVM TZ 운영 정책 확인 후 전역 지정
- **home displayOrder 경합, HomeContent 최초 insert 경합 (낮음)**: 관리자 전용 저빈도 — 후순위
- **reservation 인메모리 태스크 핸들 미보관 (중간)**: 예약 변경/게시글 삭제 시 이전 태스크 미취소(재시작 재등록은 ReservationStartupLoader가 있어 정상). 태스크 핸들 관리 설계 필요
- **PostPublishRunner 발행 race (중간, Wave 4 심사 발견)**: publishPost가 post·reservation을 무잠금 조회 + 상태 가드 없이 publish() — 발행 시각과 예약 변경 tx가 겹치면 취소된 예약을 발행하는 창. 후속: post 행 락 앵커 + 상태 재검증 (3b155e6e와 동일 패턴)
- **락 순서 규약 (낮음)**: 제명/팀삭제 리스너에 @Order로 activity(record)→score 순서 고정 — patch/delete 경로와 정렬해 이론적 데드락 창 제거. 잠금 조회(getPostForUpdate 등)가 query 계층에 노출되는 오염 반복 — 잠금 조회 소속 계층 원칙 수립 필요
- **reservation PUBLISHED 재예약 가드/중복 RESERVED**: Wave 4에서 수정 진행 중
- **activity ActivityRecord.prefixSum 행 스냅샷 미갱신 (중간)**: patch/delete 후 후속 기록 행들의 스냅샷이 옛 값 — 사용자 활동내역 화면 누적합 열 불일치. 재계산 정책 필요 (위 "prefixSum 스냅샷" 항목과 동일 건, 사용자 노출 확인됨)
- **admin/manager 경로 권한 미분리 (중간)**: `/v1/admin/**`와 `/v1/manager/**`가 동일하게 ADMIN/PRESIDENT/MANAGER 허용(PermitUrlConfig) — MANAGER가 admin 전용 의도 기능(활동기록 삭제 등)에 접근 가능. 분리 여부 정책
- **ActiveGeneration 기수 전환 직렬화 부재 (낮음)**: 단일 행 갱신+전 회원 동기화에 락 없음 — 동시 전환 시 부분 동기화. 관리자 저빈도
- **activity V1 생성 category no-op 삼항식 + V2와 표시 불일치 (낮음)**: V1 기록은 category=null로 저장돼 목록 표시가 V2와 다름 — 표시 정책 확인 후 정리

## 운영 규칙

- **dev에 직접 머지 금지** — 모든 작업은 `refactor/*` 브랜치, 머지는 팀 리뷰 후 결정
- 구조 이동 PR과 버그 수정 PR은 절대 섞지 않는다
- Phase 3부터 기능 개발과 병행 가능 (도메인 단위 PR)
