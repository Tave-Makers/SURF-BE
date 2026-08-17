# 도메인 개요

## Admin 핵심 도메인 (2기 보완 예정)

---

### member

**역할**: 회원 가입/승인/권한 관리. Admin 페이지의 핵심 도메인.

**회원 상태 흐름**

```
REGISTERING → WAITING → APPROVED
                      ↘ REJECTED
APPROVED    →          WITHDRAWN
```

**회원 권한(Role)**

| Role | 설명 | 접근 가능 API |
|------|------|---------------|
| `ADMIN` | 루트 관리자 | 전체 |
| `PRESIDENT` | 회장 | `/v1/admin/`, `/v1/manager/` |
| `MANAGER` | 매니저 | `/v1/manager/` |
| `MEMBER` | 일반 회원 | `/v1/user/` |

**회원 타입(MemberType)**: `OB` (졸업) / `YB` (재학)

**Admin이 할 수 있는 것** (`MemberAdminUsecase`)
- 가입 대기 회원 승인/거절 → 승인 시 `PersonalActivityScore` 자동 생성
- 회원 권한(Role) 변경
- 회원 상세 정보 조회 (점수 포함)
- 관리자 페이지 전용 로그인 (이메일/비밀번호)

**관련 코드**
- `controller/AdminAuthController`, `AdminMemberController`, `MemberApprovalController`
- `usecase/MemberAdminUsecase`

---

### activity

**역할**: 활동 기록 생성/조회/수정/삭제. 점수 부여의 실질적 입력 도메인.

**구조 분리 이유**

```
activity/
├── activeGeneration/   # 현재 활동 기수 설정 (기수 전환 시 사용)
└── activityRecord/     # 실제 활동 기록 CRUD
```

**활동 분류 체계**

```
ActivityCategory (대분류)
├── REGULAR_SESSION  정규 행사
├── ACTIVITY         일반 활동
├── STUDY_ON_PERSONAL / PROJECT_ON_PERSONAL   개인 스터디/프로젝트
└── STUDY_ON_TEAM / PROJECT_ON_TEAM           팀 스터디/프로젝트

ActivityType (세부 항목, 약 60여 개)
├── REWARD (상점): EARLY_BIRD(+5), WRITE_WIL(+3), CREATE_SOCIAL_CLUB(+15) 등
└── PENALTY (벌점): SESSION_ABSENCE(-30), SESSION_TRUANCY(-100) 등

AppliedTarget: INDIVIDUAL(개인) / TEAM(팀)
```

**활동기록 생성 흐름** (`ActivityRecordUsecase`)
1. 대상 회원/팀의 `PersonalActivityScore` 조회
2. `ActivityType.delta` 값으로 점수 즉시 반영 (`updateScore`)
3. 반영 후 누적 점수(`prefixSum`)와 함께 `ActivityRecord` 저장

> 점수는 활동기록 생성 시 즉시 반영됨. 별도 배치 없음.

**활동기록 수정/삭제 시 점수 자동 보정**
- 수정: 기존 delta 역산 후 새 delta 적용
- 삭제: 소프트 삭제 + `appliedScore.negate()`로 점수 롤백

**관련 코드**
- `controller/activityRecord/ActivityRecordController`
- `controller/activeGeneration/ActiveGenerationPutController`
- `usecase/ActivityRecordUsecase`

---

### score

**역할**: 회원/팀별 현재 점수 보관 및 랭킹 조회.

**구조**

```
PersonalActivityScore (엔티티)
├── member / team   ← 개인 또는 팀 중 하나만 연결
└── score           ← 현재 누적 점수 (BigDecimal)
```

> 점수 변경 로직은 `score` 도메인이 아닌 `activity` 도메인의 `ActivityRecordUsecase`에서 처리.
> `score` 도메인은 조회와 랭킹 기능 담당.

**Admin이 할 수 있는 것**
- 개인/팀 점수 랭킹 조회 (`ScoreRankingUsecase`)
- 개인 점수 + 고정 활동기록 조회 (`PersonalScoreUsecase`)

**2기 개선 포인트**
- 현재 점수는 활동기록 기반으로만 변경 가능 → 수동 점수 조정 기능 미구현
- 랭킹 캐싱 미적용

**관련 코드**
- `controller/PersonalScoreController`, `ScoreRankingGetController`
- `usecase/PersonalScoreUsecase`, `ScoreRankingUsecase`
- `utils/ScoreCalculator`

---

### badge

**역할**: 배지 생성/수정/삭제 및 회원에게 배지 부여/회수.

**구조**

```
Badge          ← 배지 정의 (이름, 이미지 등)
MemberBadge    ← 회원-배지 매핑 (다대다)
```

**Admin이 할 수 있는 것** (`BadgeUsecase`, `MemberBadgeUsecase`)
- 배지 생성/수정/삭제
- 배지를 특정 회원들에게 일괄 부여 (`assign`)
- 배지 회수 (`revoke`)
- 특정 배지 보유 회원 목록 조회
- 특정 회원의 전체 배지 조회

**현재 한계 및 2기 개선 포인트**
- 배지 자동 부여 로직 없음 → 현재는 Admin이 수동으로만 부여
- 배지 조건(점수 기준 등) 정의 기능 미구현

**관련 코드**
- `controller/BadgeCreateController`, `MemberBadgeAssignController` 등
- `usecase/BadgeUsecase`, `MemberBadgeUsecase`

---

## 일반 도메인

| 도메인 | 설명 | 주요 API |
|--------|------|---------|
| `post` | 게시글 CRUD, 좋아요, 검색, 이미지 업로드 | `/v1/user/posts` |
| `comment` | 댓글/대댓글, 좋아요 | `/v1/user/comments` |
| `board` | 게시판/카테고리 관리 | `/v1/admin/boards` |
| `schedule` | 일정 관리 (캘린더) | `/v1/user/schedules` |
| `scrap` | 게시글 스크랩 | `/v1/user/scraps` |
| `letter` | 쪽지 발신/수신 | `/v1/user/letters` |
| `notification` | FCM 푸시 알림 수신 | `/v1/user/notifications` |
| `home` | 홈 화면 배너/콘텐츠 | `/v1/user/home` |
| `feedback` | 사용자 피드백 제출 | `/v1/user/feedbacks` |
| `reservation` | 예약 게시글 관리 | `/v1/user/reservations` |
| `team` | 팀/그룹 관리 | `/v1/user/teams` |
| `login` | 카카오 OAuth2 로그인 | `/login/` |
| `block` | 회원 간 차단 등록/해제/목록 (관리자 강제 해제 포함) | `/v1/user/blocks`, `/v1/admin/blocks` |

---

### block

**역할**: 폭언·괴롭힘 사용자로부터 사용자를 보호하는 안전 기능. 차단하면 상대의 게시글·댓글이 조회에서 사라지고 쪽지·개인 알림이 차단된다.

**구조**

```
Block   ← 단방향 관계 (blocker_id → blocked_id)
```

A→B와 B→A는 서로 다른 레코드다. 한쪽을 지워도 반대 방향은 남는다. 변경되는 속성이 없어 `BaseEntity`를 상속하지 않고 `createdAt`만 둔다.

**두 정책의 방향이 다르다 — 바꿔 쓰면 동작이 조용히 바뀐다**

| 용도 | 방향 | 조회 메서드 |
|------|------|------------|
| 콘텐츠 숨김 (게시글·댓글·스크랩) | **단방향** — 내가 차단한 사람만 안 보인다. 상대 시점에서는 내 글이 그대로 보인다 | `BlockGetService.getMyBlockedMemberIds` |
| 상호작용 차단 (쪽지·개인 알림) | **양방향** — 어느 한쪽이라도 차단했으면 막는다 | `BlockGetService.existsBetween` |
| 회원 검색 표기 | 단방향, **제외하지 않고 표기만** | `BlockGetService.getMyBlockedIdsRaw` |

**sentinel 계약**: `getMyBlockedMemberIds`는 절대 빈 컬렉션을 반환하지 않는다. 차단이 0건이면 `-1L`을 넣는다 — 빈 컬렉션을 JPQL `not in`에 넘기면 공급자·DB별로 목록이 통째로 비거나 문법 오류가 난다. Java `contains` 판정에는 sentinel이 없는 `getMyBlockedIdsRaw`를 쓴다.

**적용 지점**

| 화면 | 동작 |
|------|------|
| 게시판 목록·카테고리·검색 | 쿼리 `not in`으로 제외 (조회 후 Java 필터링 금지 — 페이지 크기가 깨진다) |
| 게시글 상세 | 404. 403이면 차단 대상의 글이 존재한다는 사실이 드러난다. **조회수 증가보다 앞**에 둔다 |
| 댓글 목록·count | 같은 제외 집합을 둘 다에 넘긴다. 다르면 "댓글 3개"인데 목록이 비는 불일치가 생긴다 |
| 댓글 API 직접 호출 | `PostGetService.validateVisiblePost`로 상세 가드 우회를 막는다 |
| 스크랩 목록 | 차단 **이전**에 스크랩한 글도 사라진다. 단 스크랩 레코드는 지우지 않아 해제하면 복구된다 |
| 쪽지 | 저장·이벤트·메일 발송 **이전**에 403 |
| 개인 알림 | `Notification` 저장 이전에 차단 (FCM만 막으면 알림함에 이름이 쌓인다). 공지 알림은 제외 |
| 회원 검색 | **제외하지 않는다.** `blockedByMe` 플래그만 내려주고 `totalCount`는 유지 — 검색에서 지우면 차단을 해제할 방법이 없어진다 |

**제명(hard delete) 연동**: `BlockMemberDismissListener`가 같은 트랜잭션에서 양방향 차단 관계를 정리한다. 운영 FK가 RESTRICT라 이 정리가 빠지면 회원 삭제 자체가 실패한다 — 부수효과가 아니라 선행 조건이므로 `@Async`·AFTER_COMMIT을 쓰지 않는다.

**운영 스키마**: `block` 테이블은 `ddl-auto`가 FK를 만들지 못한다(회원을 scalar ID로 보유). 운영 DDL을 수동 적용하며 절차는 `docs/block-feature-plan.md` §13.2 참고.

**관련 코드**
- `presentation/block/controller/` — `BlockCreateController`, `BlockDeleteController`, `BlockGetController`, `BlockAdminGetController`, `BlockAdminDeleteController`
- `application/block/usecase/` — `BlockUsecase`, `BlockAdminUsecase`
- `application/block/query/` — `BlockGetService`(사용자), `BlockAdminGetService`(관리자, 방향 무관)
- `application/block/event/` — `BlockMemberDismissListener`, `BlockForceReleasedLogListener`
- `domain/block/` — `Block`, `BlockRepository`, `BlockCreateService`, `BlockDeleteService`
