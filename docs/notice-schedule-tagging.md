# 공지사항 일정 태그 API

일정은 캘린더에서 생성하고, 공지는 기존 일정 하나를 선택하여 태그한다.
일정 하나에는 여러 공지를 연결할 수 있다. 기존 공지 작성·수정 권한은 유지된다.
일정 태그는 공지사항 게시판에서만 가능하다.

## 공지 생성 및 수정

- 생성: POST /v1/user/posts
- 수정: PATCH /v1/user/posts/{postId}

기존 요청 필드에 선택적인 양수 scheduleId를 추가한다.

| 요청 | 생성 | 수정 |
|---|---|---|
| scheduleId: 5, hasSchedule 생략 또는 true | 기존 일정 5 연결 | 일정 5로 연결·교체 |
| scheduleId 생략/null, hasSchedule 생략/null | 일정 없이 생성 | 기존 연결 유지 |
| scheduleId 생략/null, hasSchedule: false | 일정 없이 생성 | 태그 해제 |
| scheduleId 존재, hasSchedule: false | 400 | 400 |
| scheduleId 생략/null, hasSchedule: true | 400 | 400 |
| 존재하지 않는 scheduleId | 404 | 404 |
| 공지사항 외 게시판에서 scheduleId 지정 | 400 | 400 |

연결·교체 요청 예시(다른 게시글 필드는 기존 계약대로 사용):

```json
{"scheduleId": 5}
```

태그 해제 요청:

```json
{"hasSchedule": false}
```

hasSchedule: true만 보내던 기존 클라이언트는 scheduleId를 함께 보내야 한다.
수정 시 scheduleId: null만 보내는 것은 연결 유지이며, 해제하려면 hasSchedule: false가 필요하다.
응답의 hasSchedule과 scheduleId는 실제 연결 상태를 반환한다.
공지 저장과 일정 연결은 하나의 트랜잭션에서 처리된다.

## 태그할 일정 목록

기존 GET /v1/user/calendar/schedules?year=2026&month=6을 재사용한다.
작성 화면에서는 월별 일정 목록의 scheduleId를 선택해 공지 저장 요청에 포함한다.
이미 공지에 연결된 일정도 선택할 수 있다. 일정 목록은 시작 시간·ID 오름차순이다.
일반 회원의 regular/other 카테고리 필터는 유지한다.

## 캘린더 응답

월별 조회 및 일정 단건 조회의 기존 단일 postId를 posts 목록으로 변경한다.
posts는 예약 공지와 사용자가 차단한 작성자의 공지를 제외하며, 작성 시간·ID 최신순이다.
연결된 공개 공지가 없어도 일정 자체는 캘린더에 표시된다.

```json
{
  "scheduleId": 5,
  "category": "regular",
  "title": "만남의 장",
  "startAt": "2026-06-20T14:00:00",
  "endAt": "2026-06-20T18:00:00",
  "location": "추후 공지",
  "mappedByPost": true,
  "posts": [
    {"postId": 12, "title": "만남의 장 추가 안내"},
    {"postId": 11, "title": "만남의 장 공지"}
  ]
}
```

연결된 조회 가능 공지가 없으면 posts: [], mappedByPost: false.
홈 화면의 일정 바로가기는 같은 필터를 적용한 공지 중 작성 시간이 가장 오래된 공지를 사용한다. 작성 시간이 같으면 ID가 작은 공지를 선택한다. 태그한 순서가 아닌 공지 작성 시간을 기준으로 하며, 해당 공지가 삭제되거나 태그 해제되면 다음으로 오래된 공개 공지로 이동한다.

## 생성·삭제 API

유지:
- POST /v1/admin/calendar/schedules: 캘린더에서 일정 생성
- PATCH /v1/admin/schedules/{scheduleId}: 일정 수정
- DELETE /v1/admin/schedules/{scheduleId}: 모든 연결 공지의 태그 해제 후 일정 삭제
- GET /v1/user/post/{postId}/schedule: 공지에 연결된 일정 조회
- GET /v1/admin/calendar/schedules/{scheduleId}: 일정 단건 조회

제거:
- POST /v1/admin/posts/{postId}/schedules: 공지 작성 중 새 일정 생성
- DELETE /v1/admin/posts/{postId}/schedules/{scheduleId}: 공지에 붙은 일정 자체 삭제

공지 삭제와 태그 해제는 일정을 삭제하지 않는다.
일정 삭제는 예약 공지를 포함한 모든 공지의 scheduleId와 hasSchedule을 정리하며 공지는 유지한다.

## 기존 DB 반영

post.schedule_id 컬럼을 그대로 사용하여 Post → Schedule 다대일 외래 키로 전환한다.
schedule.post_id는 더 이상 사용하지 않는다. Hibernate ddl-auto: update는 기존 컬럼과
외래 키를 자동으로 제거하지 않으므로, 이전 외래 키가 남으면 공지 삭제가 실패할 수 있다.

기존 DB 적용 시 연결 데이터를 유지하면서 schedule.post_id와 해당 외래 키를 정리하고, post.schedule_id 외래 키를 확인해야 한다. 별도의 SQL 파일은 제공하지 않는다.

## 검증

관련 단위·JPA 테스트를 추가했다. 최종 빌드와 테스트 실행은 담당자가 수행한다.

권장 실행:
./gradlew test --tests "*Schedule*" --tests "*PostUpdateReqDTOTest" --tests "*CleanArchitectureRulesTest"
./gradlew test --tests "*HomeUsecasePilotTest"
