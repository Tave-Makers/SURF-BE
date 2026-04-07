#!/bin/bash
# ============================================================
# QA Common Script - 공통 설정 및 유틸리티
# ============================================================

# 설정 로드
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="http://localhost:8080"  # www 없이 사용 (307 리다이렉트 방지)
TEST_IDS_FILE="${SCRIPT_DIR}/test-ids.json"

# 결과 디렉토리 생성
mkdir -p "${SCRIPT_DIR}/results"

# 토큰 확인
if [ -z "$QA_TOKEN" ]; then
  echo "ERROR: QA_TOKEN이 설정되지 않았습니다."
  echo "  export QA_TOKEN='your-token' 실행 후 다시 시도하세요."
  exit 1
fi

# ============================================================
# 테스트 ID 로드 함수
# ============================================================
load_test_ids() {
  if [ -f "$TEST_IDS_FILE" ]; then
    if command -v jq &> /dev/null; then
      # jq 사용
      TEST_BOARD_ID=$(jq -r '.resources.board.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
      TEST_CATEGORY_ID=$(jq -r '.resources.category.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
      TEST_POST_ID=$(jq -r '.resources.post.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
      TEST_COMMENT_ID=$(jq -r '.resources.comment.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
      TEST_SCHEDULE_ID=$(jq -r '.resources.schedule.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
      TEST_BANNER_ID=$(jq -r '.resources.banner.id // empty' "$TEST_IDS_FILE" 2>/dev/null)
    else
      # grep 사용 (jq 없을 때)
      TEST_BOARD_ID=$(grep -o '"board"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
      TEST_CATEGORY_ID=$(grep -o '"category"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
      TEST_POST_ID=$(grep -o '"post"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
      TEST_COMMENT_ID=$(grep -o '"comment"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
      TEST_SCHEDULE_ID=$(grep -o '"schedule"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
      TEST_BANNER_ID=$(grep -o '"banner"[^}]*"id":[^,}]*' "$TEST_IDS_FILE" 2>/dev/null | grep -o '[0-9]*' | head -1)
    fi

    # null 문자열을 빈 문자열로 변환
    [ "$TEST_BOARD_ID" = "null" ] && TEST_BOARD_ID=""
    [ "$TEST_CATEGORY_ID" = "null" ] && TEST_CATEGORY_ID=""
    [ "$TEST_POST_ID" = "null" ] && TEST_POST_ID=""
    [ "$TEST_COMMENT_ID" = "null" ] && TEST_COMMENT_ID=""
    [ "$TEST_SCHEDULE_ID" = "null" ] && TEST_SCHEDULE_ID=""
    [ "$TEST_BANNER_ID" = "null" ] && TEST_BANNER_ID=""

    return 0
  else
    # test-ids.json 없을 때 API로 최신 ID 동적 조회
    TEST_BOARD_ID="2"
    TEST_CATEGORY_ID="1"

    # 최신 게시글 ID 조회
    _posts_resp=$(curl -s -H "Authorization: Bearer $QA_TOKEN" \
      "${BASE_URL}/v1/user/posts?boardId=${TEST_BOARD_ID}&page=0&size=1" 2>/dev/null)
    TEST_POST_ID=$(echo "$_posts_resp" | jq -r '.data.content[0].postId // empty' 2>/dev/null)

    # 게시글이 없으면 새로 생성
    if [ -z "$TEST_POST_ID" ] || [ "$TEST_POST_ID" = "null" ]; then
      _create_resp=$(curl -s -X POST \
        -H "Authorization: Bearer $QA_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"boardId\":${TEST_BOARD_ID},\"categoryId\":${TEST_CATEGORY_ID},\"title\":\"[QA Test] 자동화 테스트 게시글\",\"content\":\"QA 테스트용\",\"pinned\":false,\"hasSchedule\":false}" \
        "${BASE_URL}/v1/user/posts" 2>/dev/null)
      TEST_POST_ID=$(echo "$_create_resp" | jq -r '.data.postId // empty' 2>/dev/null)
    fi

    # 최신 댓글 ID 조회
    if [ -n "$TEST_POST_ID" ] && [ "$TEST_POST_ID" != "null" ]; then
      _comments_resp=$(curl -s -H "Authorization: Bearer $QA_TOKEN" \
        "${BASE_URL}/v1/user/posts/${TEST_POST_ID}/comments?page=0&size=1" 2>/dev/null)
      TEST_COMMENT_ID=$(echo "$_comments_resp" | jq -r '.data.content[0].id // empty' 2>/dev/null)

      # 댓글이 없으면 새로 생성
      if [ -z "$TEST_COMMENT_ID" ] || [ "$TEST_COMMENT_ID" = "null" ]; then
        _create_comment=$(curl -s -X POST \
          -H "Authorization: Bearer $QA_TOKEN" \
          -H "Content-Type: application/json" \
          -d '{"content":"[QA Test] 자동화 테스트 댓글"}' \
          "${BASE_URL}/v1/user/posts/${TEST_POST_ID}/comments" 2>/dev/null)
        TEST_COMMENT_ID=$(echo "$_create_comment" | jq -r '.data.id // empty' 2>/dev/null)
        # 댓글 좋아요 추가
        curl -s -X POST -H "Authorization: Bearer $QA_TOKEN" \
          "${BASE_URL}/v1/user/comments/${TEST_COMMENT_ID}/like" > /dev/null 2>&1
      fi
    fi

    TEST_SCHEDULE_ID=""
    TEST_BANNER_ID=""
    return 1
  fi
}

# ============================================================
# HTTP 요청 유틸리티 함수
# ============================================================

# GET 요청
qa_get() {
  local path="$1"
  local auth="${2:-true}"

  if [ "$auth" = "true" ]; then
    curl -s -w "\n%{http_code}" -X GET \
      -H "Authorization: Bearer $QA_TOKEN" \
      -H "Content-Type: application/json" \
      "${BASE_URL}${path}"
  else
    curl -s -w "\n%{http_code}" -X GET \
      -H "Content-Type: application/json" \
      "${BASE_URL}${path}"
  fi
}

# POST 요청
qa_post() {
  local path="$1"
  local data="$2"
  local auth="${3:-true}"

  if [ "$auth" = "true" ]; then
    curl -s -w "\n%{http_code}" -X POST \
      -H "Authorization: Bearer $QA_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "${BASE_URL}${path}"
  else
    curl -s -w "\n%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -d "$data" \
      "${BASE_URL}${path}"
  fi
}

# 응답에서 HTTP 코드 추출
get_http_code() {
  echo "$1" | tail -n1
}

# 응답에서 바디 추출
get_body() {
  echo "$1" | sed '$d'
}

# 테스트 결과 판정 (2xx면 성공)
is_success() {
  local http_code="$1"
  [[ "$http_code" =~ ^2 ]]
}

# ============================================================
# 테스트 ID 로드 (스크립트 시작 시)
# ============================================================
load_test_ids

# 로드된 ID 출력 (디버그용)
if [ -n "$DEBUG" ]; then
  echo "[DEBUG] Test IDs loaded:"
  echo "  - Board ID: ${TEST_BOARD_ID:-N/A}"
  echo "  - Category ID: ${TEST_CATEGORY_ID:-N/A}"
  echo "  - Post ID: ${TEST_POST_ID:-N/A}"
  echo "  - Comment ID: ${TEST_COMMENT_ID:-N/A}"
  echo "  - Schedule ID: ${TEST_SCHEDULE_ID:-N/A}"
  echo "  - Banner ID: ${TEST_BANNER_ID:-N/A}"
fi

# ============================================================
# 파괴적 API 스킵 체크 함수
# ============================================================
SKIP_LIST_FILE="${SCRIPT_DIR}/.skip-apis"

# API 경로가 스킵 목록에 있는지 확인
should_skip_api() {
  local path="$1"

  if [ ! -f "$SKIP_LIST_FILE" ]; then
    return 1  # 스킵 목록 없음 = 실행
  fi

  # 경로에서 {변수} 부분을 정규식 패턴으로 변환하여 매칭
  while IFS= read -r skip_pattern; do
    # {postId} 같은 패턴을 .* 로 변환
    regex_pattern=$(echo "$skip_pattern" | sed 's/{[^}]*}/[^/]*/g')

    if [[ "$path" =~ ^${regex_pattern}$ ]]; then
      return 0  # 스킵해야 함
    fi
  done < "$SKIP_LIST_FILE"

  return 1  # 실행해야 함
}

# 스킵 여부를 확인하고 메시지 출력
check_and_skip() {
  local method="$1"
  local path="$2"
  local description="$3"

  if should_skip_api "$path"; then
    echo "⏭️ SKIP: $description ($method $path) - 파괴적 API"
    return 0  # 스킵됨
  fi

  return 1  # 실행해야 함
}
