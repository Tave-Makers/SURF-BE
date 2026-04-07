#!/bin/bash
# ============================================================
# QA Setup Script - 테스트 데이터 생성
# ============================================================
# 테스트에 필요한 리소스를 생성하고 ID를 저장합니다.
# 생성 순서: Board 조회 → Post 생성 → Comment 생성 → Schedule 생성
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${QA_BASE_URL:-http://localhost:8080}"
TEST_IDS_FILE="${SCRIPT_DIR}/test-ids.json"

echo "============================================================"
echo "  QA Setup - 테스트 데이터 생성"
echo "============================================================"
echo ""

# 토큰 확인
if [ -z "$QA_TOKEN" ]; then
  echo "ERROR: QA_TOKEN이 설정되지 않았습니다."
  echo "  export QA_TOKEN='your-token' 실행 후 다시 시도하세요."
  exit 1
fi

echo "[1/6] QA용 게시판 및 카테고리 조회 중..."
# 관리자 API로 QA용 게시판 생성 (없으면 기존 것 재사용)
boards_response=$(curl -s -X GET \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "Content-Type: application/json" \
  "${BASE_URL}/v1/user/boards")

# 전체 게시판 중 마지막 id 추출 (QA용 게시판 우선)
if command -v jq &> /dev/null; then
  # QA용 게시판 찾기
  QA_BOARD_ID=$(echo "$boards_response" | jq -r '[.data[] | select(.name == "QA테스트게시판")] | .[0].id // empty' 2>/dev/null)

  if [ -z "$QA_BOARD_ID" ] || [ "$QA_BOARD_ID" = "null" ]; then
    # QA용 게시판 생성
    create_board_resp=$(curl -s -X POST \
      -H "Authorization: Bearer $QA_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"name":"QA테스트게시판","type":"NOTICE"}' \
      "${BASE_URL}/v1/admin/boards")
    BOARD_ID=$(echo "$create_board_resp" | jq -r '.data.id // 1' 2>/dev/null || echo "1")
  else
    BOARD_ID=$QA_BOARD_ID
  fi

  # 해당 게시판의 카테고리 조회
  board_detail=$(curl -s -X GET \
    -H "Authorization: Bearer $QA_TOKEN" \
    "${BASE_URL}/v1/admin/boards/${BOARD_ID}")
  CATEGORY_ID=$(echo "$board_detail" | jq -r '.data.categories[0].id // .data.categories[0].categoryId // empty' 2>/dev/null)
else
  BOARD_ID=2
  CATEGORY_ID=1
fi

# 카테고리가 없으면 DB에 직접 INSERT
if [ -z "$CATEGORY_ID" ] || [ "$CATEGORY_ID" = "null" ]; then
  echo "  - 카테고리 없음, DB에 직접 생성..."
  mysql -u root -ppw930516 SURF2 -e \
    "INSERT IGNORE INTO board_category (board_id, name, slug, created_at, updated_at) VALUES (${BOARD_ID}, 'QA테스트카테고리', 'qa-test', NOW(), NOW());" 2>/dev/null
  CATEGORY_ID=$(mysql -u root -ppw930516 SURF2 -se \
    "SELECT id FROM board_category WHERE board_id=${BOARD_ID} AND slug='qa-test' LIMIT 1;" 2>/dev/null)
fi

# 기본값 설정
BOARD_ID=${BOARD_ID:-2}
CATEGORY_ID=${CATEGORY_ID:-1}

echo "  - Board ID: $BOARD_ID"
echo "  - Category ID: $CATEGORY_ID"
echo ""

echo "[2/6] 테스트 게시글 생성 중..."
# 테스트 게시글 생성
post_response=$(curl -s -X POST \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "boardId": '"$BOARD_ID"',
    "categoryId": '"$CATEGORY_ID"',
    "title": "[QA Test] 자동화 테스트 게시글",
    "content": "이 게시글은 QA 자동화 테스트를 위해 생성되었습니다. 테스트 완료 후 자동 삭제됩니다.",
    "pinned": false,
    "hasSchedule": false
  }' \
  "${BASE_URL}/v1/user/posts" 2>/dev/null)

# postId 추출
if command -v jq &> /dev/null; then
  POST_ID=$(echo "$post_response" | jq -r '.data.postId // .data.id // .postId // .id // empty' 2>/dev/null)
else
  POST_ID=$(echo "$post_response" | grep -o '"postId":[0-9]*' | head -1 | grep -o '[0-9]*')
  if [ -z "$POST_ID" ]; then
    POST_ID=$(echo "$post_response" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  fi
fi

if [ -z "$POST_ID" ] || [ "$POST_ID" = "null" ]; then
  echo "  WARNING: 게시글 생성 실패, 기존 게시글 조회 시도..."

  # 기존 게시글에서 가져오기
  posts_response=$(curl -s -X GET \
    -H "Authorization: Bearer $QA_TOKEN" \
    -H "Content-Type: application/json" \
    "${BASE_URL}/v1/user/posts?boardId=${BOARD_ID}&page=0&size=1")

  if command -v jq &> /dev/null; then
    POST_ID=$(echo "$posts_response" | jq -r '.data.content[0].postId // .data[0].postId // .data[0].id // empty' 2>/dev/null)
  else
    POST_ID=$(echo "$posts_response" | grep -o '"postId":[0-9]*' | head -1 | grep -o '[0-9]*')
  fi

  POST_CREATED="false"
  echo "  - 기존 Post ID 사용: $POST_ID"
else
  POST_CREATED="true"
  echo "  - Post ID: $POST_ID (새로 생성됨)"
fi
echo ""

echo "[3/6] 테스트 댓글 생성 중..."
# 테스트 댓글 생성
if [ -n "$POST_ID" ] && [ "$POST_ID" != "null" ]; then
  comment_response=$(curl -s -X POST \
    -H "Authorization: Bearer $QA_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "content": "[QA Test] 자동화 테스트 댓글입니다."
    }' \
    "${BASE_URL}/v1/user/posts/${POST_ID}/comments" 2>/dev/null)

  if command -v jq &> /dev/null; then
    COMMENT_ID=$(echo "$comment_response" | jq -r '.data.commentId // .data.id // .commentId // .id // empty' 2>/dev/null)
  else
    COMMENT_ID=$(echo "$comment_response" | grep -o '"commentId":[0-9]*' | head -1 | grep -o '[0-9]*')
    if [ -z "$COMMENT_ID" ]; then
      COMMENT_ID=$(echo "$comment_response" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
    fi
  fi

  if [ -z "$COMMENT_ID" ] || [ "$COMMENT_ID" = "null" ]; then
    echo "  WARNING: 댓글 생성 실패, 기존 댓글 조회 시도..."

    comments_response=$(curl -s -X GET \
      -H "Authorization: Bearer $QA_TOKEN" \
      -H "Content-Type: application/json" \
      "${BASE_URL}/v1/user/posts/${POST_ID}/comments?page=0&size=1")

    if command -v jq &> /dev/null; then
      COMMENT_ID=$(echo "$comments_response" | jq -r '.data.content[0].commentId // .data[0].commentId // .data[0].id // empty' 2>/dev/null)
    else
      COMMENT_ID=$(echo "$comments_response" | grep -o '"commentId":[0-9]*' | head -1 | grep -o '[0-9]*')
    fi

    COMMENT_CREATED="false"
    echo "  - 기존 Comment ID 사용: $COMMENT_ID"
  else
    COMMENT_CREATED="true"
    echo "  - Comment ID: $COMMENT_ID (새로 생성됨)"
  fi
else
  COMMENT_ID=""
  COMMENT_CREATED="false"
  echo "  - Comment 생성 건너뜀 (Post ID 없음)"
fi
echo ""

echo "[4/6] 테스트 일정 생성 중..."
# 테스트 일정 생성 (관리자 API)
START_DATE=$(date -u +"%Y-%m-%dT10:00:00")
END_DATE=$(date -u +"%Y-%m-%dT12:00:00")

schedule_response=$(curl -s -X POST \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "MEETING",
    "title": "[QA Test] 자동화 테스트 일정",
    "startAt": "'"$START_DATE"'",
    "endAt": "'"$END_DATE"'",
    "location": "테스트 장소"
  }' \
  "${BASE_URL}/v1/admin/calendar/schedules" 2>/dev/null)

if command -v jq &> /dev/null; then
  SCHEDULE_ID=$(echo "$schedule_response" | jq -r '.data.scheduleId // .data.id // .scheduleId // .id // empty' 2>/dev/null)
else
  SCHEDULE_ID=$(echo "$schedule_response" | grep -o '"scheduleId":[0-9]*' | head -1 | grep -o '[0-9]*')
  if [ -z "$SCHEDULE_ID" ]; then
    SCHEDULE_ID=$(echo "$schedule_response" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  fi
fi

if [ -z "$SCHEDULE_ID" ] || [ "$SCHEDULE_ID" = "null" ]; then
  SCHEDULE_ID=""
  SCHEDULE_CREATED="false"
  echo "  WARNING: 일정 생성 실패 (관리자 권한 필요할 수 있음)"
else
  SCHEDULE_CREATED="true"
  echo "  - Schedule ID: $SCHEDULE_ID (새로 생성됨)"
fi
echo ""

echo "[5/6] 테스트 배너 생성 중..."
# 테스트 배너 생성 (관리자 API)
banner_response=$(curl -s -X POST \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://example.com/qa-test-banner.jpg",
    "linkUrl": "https://example.com/qa-test"
  }' \
  "${BASE_URL}/v1/admin/home/banners" 2>/dev/null)

if command -v jq &> /dev/null; then
  BANNER_ID=$(echo "$banner_response" | jq -r '.data.bannerId // .data.id // .bannerId // .id // empty' 2>/dev/null)
else
  BANNER_ID=$(echo "$banner_response" | grep -o '"bannerId":[0-9]*' | head -1 | grep -o '[0-9]*')
  if [ -z "$BANNER_ID" ]; then
    BANNER_ID=$(echo "$banner_response" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  fi
fi

if [ -z "$BANNER_ID" ] || [ "$BANNER_ID" = "null" ]; then
  BANNER_ID=""
  BANNER_CREATED="false"
  echo "  WARNING: 배너 생성 실패 (관리자 권한 필요할 수 있음)"
else
  BANNER_CREATED="true"
  echo "  - Banner ID: $BANNER_ID (새로 생성됨)"
fi
echo ""

echo "[6/6] 테스트 ID 저장 중..."
# test-ids.json 파일 생성
cat > "$TEST_IDS_FILE" << EOF
{
  "createdAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "baseUrl": "$BASE_URL",
  "resources": {
    "board": {
      "id": ${BOARD_ID:-null},
      "created": false
    },
    "category": {
      "id": ${CATEGORY_ID:-null},
      "created": false
    },
    "post": {
      "id": ${POST_ID:-null},
      "created": $POST_CREATED
    },
    "comment": {
      "id": ${COMMENT_ID:-null},
      "created": ${COMMENT_CREATED:-false}
    },
    "schedule": {
      "id": ${SCHEDULE_ID:-null},
      "created": ${SCHEDULE_CREATED:-false}
    },
    "banner": {
      "id": ${BANNER_ID:-null},
      "created": ${BANNER_CREATED:-false}
    }
  }
}
EOF

echo "  - 저장 완료: $TEST_IDS_FILE"
echo ""

echo "============================================================"
echo "  Setup 완료!"
echo "============================================================"
echo ""
echo "  생성된 리소스:"
echo "  - Board ID:    ${BOARD_ID:-N/A} (기존)"
echo "  - Category ID: ${CATEGORY_ID:-N/A} (기존)"
echo "  - Post ID:     ${POST_ID:-N/A} (${POST_CREATED})"
echo "  - Comment ID:  ${COMMENT_ID:-N/A} (${COMMENT_CREATED})"
echo "  - Schedule ID: ${SCHEDULE_ID:-N/A} (${SCHEDULE_CREATED})"
echo "  - Banner ID:   ${BANNER_ID:-N/A} (${BANNER_CREATED})"
echo ""
echo "  다음 단계: ./run-all.sh 또는 개별 테스트 실행"
echo "============================================================"
