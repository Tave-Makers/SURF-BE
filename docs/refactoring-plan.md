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
| 2 | badge 도메인 파일럿 전환 → 4계층 템플릿 + 도메인 점검 체크리스트 확립 | |
| 3 | 도메인별 전환 + 전 로직 점검 (아래 Wave 순서) | |
| 4 | global 정리, 베이스라인 0건 달성, FreezingArchRule 해제 | |

### Phase 1 — P0 버그 결과 (2026-07-07 완료)

1. ✅ 댓글 좋아요 lost update — `likeCount` 원자적 UPDATE + 중복 등록 race는 `CommentLikeAlreadyExistsException`(409)으로 전환
2. ✅ 활동 점수 lost update — 쓰기 경로에 PESSIMISTIC_WRITE `ForUpdate` 조회 도입 (읽기 경로는 무잠금 유지)
3. ✅ 배지 부여 race — `saveAllAndFlush`로 unique 위반을 커밋 전 감지, 도메인 예외로 변환 (트랜잭션은 원래 usecase에 있었음 — 스캔 오탐 정정)
4. ✅ FCM 무효 토큰 비활성화 유실 — `DeviceTokenUsecase`(@Transactional) 벌크 UPDATE로 교체.
   배지 회수/예약 생성/활동기록의 "@Transactional 누락" 주장은 usecase 트랜잭션 존재로 오탐 종결
5. ✅ 알림 읽음 처리 — `findByIdAndMemberId`로 소유권 필터 조회
6. ✖️ 쪽지 메일 예외 삼킴 — 오탐 (이미 에러 로그 + `LetterMailSendFailException` throw 중)

### Phase 3 이관 항목 (Phase 1 리뷰에서 도출, non-blocking)

- `PersonalScoreGetService`의 ForUpdate 메서드는 "GetService = 읽기" 계약을 흐림 → score 도메인 command 포트로 이관
- `FcmService → DeviceTokenUsecase` 계층 역행 → 개명 또는 이벤트 분리
- `LetterFacade`: 메일 발송(외부 API)이 @Transactional 안 + 저장 전 발송 순서 (R5, Phase 2 트랜잭션 경계 정리 대상)
- `ReservationUsecase`: 트랜잭션 커밋 전 스케줄 등록 → 롤백 시 좀비 job (Codex 지적과 동일, Phase 2)
- `ActivityRecordUsecase.scoreCalculator` 미사용 필드 (데드코드)
- `removeAllByMemberId` 류 반복 delete → 벌크 전환

### Phase 3 — 도메인 전환 Wave

| Wave | 도메인 | 비고 |
|------|--------|------|
| 1 | badge(파일럿), member + auth | MemberDismissUsecase 해체 → MemberWithdrawnEvent로 각 도메인이 자기 데이터 정리 |
| 2 | post, comment, scrap | 트래픽 핵심, 멱등성 이슈 밀집. scrap↔post 순환 의존 해소 포함 |
| 3 | notification, score, letter | 이벤트 리스너 패턴 정리의 중심 |
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

## 운영 규칙

- **dev에 직접 머지 금지** — 모든 작업은 `refactor/*` 브랜치, 머지는 팀 리뷰 후 결정
- 구조 이동 PR과 버그 수정 PR은 절대 섞지 않는다
- Phase 3부터 기능 개발과 병행 가능 (도메인 단위 PR)
