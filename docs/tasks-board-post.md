# 구현 태스크 — Board / Post 기능 추가

> 상세 코드 가이드는 [`docs/board-category-guide.md`](./board-category-guide.md) 참고

---

## Task 1. 일반 게시판 타입 추가

**목적**: 공지사항(NOTICE) 외에 일반 회원도 글을 쓸 수 있는 게시판 타입이 필요합니다.

### 수정할 파일

| 파일 | 할 일 |
|------|-------|
| `domain/board/entity/BoardType.java` | `GENERAL` 값 추가 |
| `domain/board/entity/Board.java` | `isGeneral()` 메서드 추가 |
| `domain/board/exception/ErrorMessage.java` | `BOARD_WRITE_NOT_ALLOWED` 에러 메시지 추가 |
| `domain/post/service/post/PostCreateService.java` | 게시글 작성 시 NOTICE 타입 게시판이면 예외 발생 처리 추가 |

### 새로 만들 파일

| 파일 | 내용 |
|------|------|
| `domain/board/exception/BoardWriteNotAllowedException.java` | 공지 게시판 작성 시 던지는 예외 |

---

## Task 2. 하위 게시판(카테고리) 생성 API

**목적**: 기존에는 카테고리를 SQL로 직접 DB에 넣었습니다. 관리자가 API로 만들 수 있어야 합니다.

**엔드포인트**: `POST /v1/admin/boards/{boardId}/categories`

```json
// 요청
{ "name": "제휴", "slug": "partnership" }

// 응답
{ "id": 1, "name": "제휴", "slug": "partnership" }
```

### 수정할 파일

| 파일 | 할 일 |
|------|-------|
| `domain/board/entity/BoardCategory.java` | `of(Board, BoardCategoryCreateReqDTO)` 정적 팩토리 추가 |
| `domain/board/usecase/BoardUsecase.java` | `createCategory()` 메서드 추가 |
| `domain/board/controller/ResponseMessage.java` | `CATEGORY_CREATED` 메시지 추가 |

### 새로 만들 파일

| 파일 | 내용 |
|------|------|
| `domain/board/dto/request/BoardCategoryCreateReqDTO.java` | `name`, `slug` 필드 |
| `domain/board/dto/response/BoardCategoryResDTO.java` | `id`, `name`, `slug` + `from()` 팩토리 |
| `domain/board/service/BoardCategoryService.java` | `createCategory(Board, req)` |
| `domain/board/controller/BoardCategoryCreateController.java` | `POST /v1/admin/boards/{boardId}/categories` |

### 호출 흐름

```
Controller → BoardUsecase.createCategory(boardId, req)
                ├── BoardGetService.getBoard(boardId)   ← Board 존재 검증
                └── BoardCategoryService.createCategory(board, req)
                        └── boardCategoryRepository.save()
```

---

## Task 3. 게시글 첨부파일 추가

**목적**: 현재 게시글에 이미지만 첨부 가능합니다. PDF, PPT 같은 일반 파일도 첨부할 수 있어야 합니다.

> 이미지와 완전히 동일한 패턴입니다.  
> `PostImageUrl`, `PostImageCreateService` 코드를 먼저 읽고 똑같이 만들면 됩니다.

### 수정할 파일

| 파일 | 할 일 |
|------|-------|
| `domain/post/dto/request/PostCreateReqDTO.java` | `List<PostFileCreateReqDTO> fileList` 필드 + `hasFile()` 메서드 추가 |
| `domain/post/service/post/PostCreateService.java` | 파일 저장 로직 추가 (`hasFile()` 이면 `PostFileCreateService.saveAll()` 호출) |
| `domain/post/dto/response/PostDetailResDTO.java` | `List<PostFileResDTO> fileList` 필드 추가, `of()` 시그니처 수정 |

> `PostDetailResDTO.of()`를 호출하는 곳이 여러 군데입니다. IDE에서 `PostDetailResDTO.of(` 전체 검색해서 모두 수정하세요.

### 새로 만들 파일

| 파일 | 참고할 기존 파일 |
|------|-----------------|
| `domain/post/dto/request/PostFileCreateReqDTO.java` | `PostImageCreateReqDTO.java` |
| `domain/post/dto/response/PostFileResDTO.java` | `PostImageResDTO.java` |
| `domain/post/entity/PostFileUrl.java` | `PostImageUrl.java` |
| `domain/post/repository/PostFileUrlRepository.java` | `PostImageUrlRepository.java` |
| `domain/post/service/image/PostFileCreateService.java` | `PostImageCreateService.java` |

---

## 시작 전 읽어야 할 파일 (필수)

코드 패턴을 이해하기 위해 아래 파일들을 순서대로 읽어보세요.

1. `domain/board/entity/Board.java` — 엔티티 + 정적 팩토리 패턴
2. `domain/board/service/BoardService.java` — Service 패턴
3. `domain/board/usecase/BoardUsecase.java` — Usecase 패턴
4. `domain/board/controller/BoardCreateController.java` — Controller 패턴
5. `domain/post/entity/PostImageUrl.java` — 첨부 파일 엔티티 패턴
6. `domain/post/service/image/PostImageCreateService.java` — 파일 저장 서비스 패턴
