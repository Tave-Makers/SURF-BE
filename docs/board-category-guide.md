# Board 도메인 구조 이해 및 구현 가이드

> 이 문서는 신규 팀원이 Board 도메인의 전체 흐름을 이해하고,  
> **BoardType 추가**, **하위 게시판(BoardCategory) 생성 API 구현**, **게시글 첨부파일 추가**, **게시글 예약 발행 흐름**을 파악할 수 있도록 작성되었습니다.

---

## 1. Board 도메인이 뭐야?

SURF에서 "공지사항", "자유게시판" 같은 **게시판 자체**를 관리하는 도메인입니다.

```
Board (게시판)
 └── BoardCategory (하위 게시판 / 카테고리)
      └── Post (게시글)
```

예시:
- Board: "공지사항" (type = NOTICE)
  - BoardCategory: "제휴" (slug = partnership)
  - BoardCategory: "패치" (slug = patch)

---

## 2. 현재 코드 구조 한눈에 보기

```
domain/board/
├── entity/
│   ├── Board.java             # 게시판 엔티티
│   ├── BoardCategory.java     # 하위 게시판 엔티티
│   └── BoardType.java         # 게시판 타입 Enum (현재 NOTICE만 있음)
│
├── controller/
│   ├── BoardCreateController.java   # POST /v1/admin/boards
│   ├── BoardGetController.java      # GET  /v1/admin/boards
│   ├── BoardPatchController.java    # PATCH /v1/admin/boards/{id}
│   ├── BoardDeleteController.java   # DELETE /v1/admin/boards/{id}
│   └── ResponseMessage.java         # 응답 메시지 Enum
│
├── usecase/
│   └── BoardUsecase.java      # @Transactional 관리, Service 조합
│
├── service/
│   ├── BoardService.java          # Board CRUD 비즈니스 로직
│   ├── BoardGetService.java       # Board 엔티티 조회 전용 (타 도메인에서도 사용)
│   └── BoardCategoryGetService.java  # BoardCategory 조회 전용 (타 도메인에서도 사용)
│
├── repository/
│   ├── BoardRepository.java
│   └── BoardCategoryRepository.java
│
├── dto/
│   ├── request/
│   │   ├── BoardCreateReqDTO.java
│   │   └── BoardUpdateReqDTO.java
│   └── response/
│       └── BoardResDTO.java
│
└── exception/
    ├── BoardNotFoundException.java
    ├── CategoryNotFoundException.java
    ├── CategoryRequiredException.java
    ├── InvalidCategoryMappingException.java
    └── ErrorMessage.java          # 에러 메시지 Enum
```

---

## 3. 요청이 들어오면 어떤 순서로 처리될까?

SURF의 계층 구조는 다음과 같습니다:

```
HTTP 요청
    ↓
Controller       → 요청을 받아 Usecase에 넘김
    ↓
Usecase          → @Transactional 관리, Service들을 조합
    ↓
Service          → 실제 비즈니스 로직 처리
    ↓
Repository       → DB 접근
```

### 실제 코드로 보기: "게시판 생성" 흐름

**① Controller** [`BoardCreateController.java`]
```java
@PostMapping("/v1/admin/boards")
public ApiResponse<BoardResDTO> createBoard(@Valid @RequestBody BoardCreateReqDTO req) {
    BoardResDTO response = boardUsecase.createBoard(req);   // ← Usecase 호출
    return ApiResponse.response(HttpStatus.CREATED, BOARD_CREATED.getMessage(), response);
}
```

**② Usecase** [`BoardUsecase.java`]
```java
@Transactional                          // ← 여기서 트랜잭션 관리
public BoardResDTO createBoard(BoardCreateReqDTO req) {
    return boardService.createBoard(req);  // ← Service 호출
}
```

**③ Service** [`BoardService.java`]
```java
@LogEvent(value = "board.create", message = "게시판 생성 성공")
public BoardResDTO createBoard(BoardCreateReqDTO req) {
    Board board = Board.of(req);           // ← 엔티티 생성 (정적 팩토리 메서드)
    Board saved = boardRepository.save(board);  // ← DB 저장
    return BoardResDTO.from(saved);        // ← DTO 변환 후 반환
}
```

> **핵심 규칙**: `@Transactional`은 **Usecase에서만** 붙입니다.  
> Service에는 붙이지 않아요. (이미 Usecase 트랜잭션 안에서 실행되기 때문)

---

## 4. 주요 클래스 설명

### Board 엔티티 [`entity/Board.java`]
```java
public class Board extends BaseEntity {
    private Long id;
    private String name;     // 게시판 이름 (ex: "공지사항")
    private BoardType type;  // 게시판 타입 (ex: NOTICE)

    public static Board of(BoardCreateReqDTO req) { ... }  // 정적 팩토리
    public boolean isNotice() { return type == BoardType.NOTICE; }
}
```

### BoardCategory 엔티티 [`entity/BoardCategory.java`]
```java
public class BoardCategory extends BaseEntity {
    private Long id;
    private Board board;     // 어떤 게시판에 속하는지
    private String name;     // 카테고리 이름 (ex: "제휴")
    private String slug;     // URL용 식별자 (ex: "partnership") - 같은 Board 내에서 unique
}
```

### BoardType Enum [`entity/BoardType.java`]
```java
public enum BoardType {
    NOTICE,   // 공지사항 - 관리자만 게시글 작성 가능
    // ← 여기에 GENERAL 추가 예정
}
```

### GetService 패턴
`BoardGetService`, `BoardCategoryGetService`는 **엔티티를 조회해서 반환**하는 전용 서비스입니다.  
다른 도메인(ex: post 도메인)에서 Board나 BoardCategory 엔티티가 필요할 때 이것을 사용합니다.

```java
// PostCreateService에서 사용 예시
Board board = boardGetService.getBoard(req.boardId());           // Board 엔티티 조회
BoardCategory category = boardCategoryGetService.getCategory(categoryId);  // Category 엔티티 조회
```

> **규칙**: 타 도메인의 Repository를 직접 불러오지 않습니다. 반드시 GetService를 통해 접근합니다.

---

## 5. 구현해야 할 것들

이번에 새로 구현할 내용은 크게 두 가지입니다.

### Task 1: `BoardType.GENERAL` 추가 + 게시글 작성 권한 분리
### Task 2: BoardCategory 생성 API 구현 (`POST /v1/admin/boards/{boardId}/categories`)

---

## Task 1: BoardType.GENERAL 추가

### 1-1. BoardType Enum에 값 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/board/entity/BoardType.java`

```java
public enum BoardType {
    NOTICE,   // 공지사항 - 관리자만 게시글 작성 가능
    GENERAL,  // 일반 게시판 - 일반 회원도 게시글 작성 가능
}
```

### 1-2. Board 엔티티에 타입 확인 메서드 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/board/entity/Board.java`

`isNotice()` 메서드 아래에 다음을 추가합니다:

```java
public boolean isGeneral() {
    return type == BoardType.GENERAL;
}
```

### 1-3. 게시글 작성 시 권한 검증 추가

게시판이 NOTICE 타입이면 일반 회원은 게시글을 작성할 수 없어야 합니다.

**파일**: `src/main/java/com/tavemakers/surf/domain/post/service/post/PostCreateService.java`

`createPost()` 메서드 안, `Board board = boardGetService.getBoard(...)` 바로 아래에 추가합니다:

```java
Board board = boardGetService.getBoard(req.boardId());

// NOTICE 게시판은 관리자만 작성 가능 (SecurityConfig에서 경로로 제어해도 되지만, 서비스 레벨에서도 방어)
if (board.isNotice()) {
    throw new BoardWriteNotAllowedException();  // 아래에서 새로 만들 예외
}
```

> **참고**: 현재 `/v1/user/posts` 경로로 게시글을 작성합니다.  
> 관리자용 공지 게시글 작성은 별도 `/v1/admin/posts` 경로가 있거나, 추후 추가될 수 있습니다.  
> 리드에게 정확한 권한 분리 방식을 확인하세요.

새 예외 클래스 생성:

**파일**: `src/main/java/com/tavemakers/surf/domain/board/exception/BoardWriteNotAllowedException.java`

```java
package com.tavemakers.surf.domain.board.exception;

import com.tavemakers.surf.global.common.exception.BaseException;
import static com.tavemakers.surf.domain.board.exception.ErrorMessage.BOARD_WRITE_NOT_ALLOWED;

public class BoardWriteNotAllowedException extends BaseException {
    public BoardWriteNotAllowedException() {
        super(BOARD_WRITE_NOT_ALLOWED.getStatus(), BOARD_WRITE_NOT_ALLOWED.getMessage());
    }
}
```

`ErrorMessage` enum에도 추가:

**파일**: `src/main/java/com/tavemakers/surf/domain/board/exception/ErrorMessage.java`

```java
BOARD_WRITE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "[공지사항] 게시판은 관리자만 게시글을 작성할 수 있습니다."),
```

---

## Task 2: BoardCategory 생성 API 구현

지금부터 실제로 새 API를 만드는 과정입니다. 기존 Board 생성 코드와 동일한 패턴을 따라가면 됩니다.

### 완성 후 파일 구조

```
domain/board/
├── controller/
│   └── BoardCategoryCreateController.java   ← 새로 생성
├── service/
│   └── BoardCategoryService.java            ← 새로 생성
├── dto/
│   ├── request/
│   │   └── BoardCategoryCreateReqDTO.java   ← 새로 생성
│   └── response/
│       └── BoardCategoryResDTO.java         ← 새로 생성
```

`BoardUsecase`와 `ResponseMessage`는 기존 파일에 코드를 **추가**합니다.

---

### Step 1: Request DTO 만들기

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/board/dto/request/BoardCategoryCreateReqDTO.java`

```java
package com.tavemakers.surf.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게시판 카테고리 생성 요청 DTO")
public record BoardCategoryCreateReqDTO(

        @Schema(description = "카테고리 이름", example = "제휴")
        @NotBlank String name,

        @Schema(description = "URL용 슬러그", example = "partnership")
        @NotBlank String slug
) {}
```

> **slug란?** URL에서 카테고리를 식별하는 문자열입니다.  
> 예: `/boards/1/partnership` 에서 `partnership` 부분입니다.  
> 같은 Board 안에서만 unique하면 됩니다.

---

### Step 2: Response DTO 만들기

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/board/dto/response/BoardCategoryResDTO.java`

```java
package com.tavemakers.surf.domain.board.dto.response;

import com.tavemakers.surf.domain.board.entity.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 카테고리 응답 DTO")
public record BoardCategoryResDTO(

        @Schema(description = "카테고리 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 이름", example = "제휴")
        String name,

        @Schema(description = "슬러그", example = "partnership")
        String slug
) {
    public static BoardCategoryResDTO from(BoardCategory category) {
        return new BoardCategoryResDTO(
                category.getId(),
                category.getName(),
                category.getSlug()
        );
    }
}
```

> **정적 팩토리 `from()`**: 엔티티 → DTO 변환을 담당합니다.  
> `BoardResDTO.java`에서도 동일한 패턴을 사용하고 있습니다. 참고하세요.

---

### Step 3: BoardCategory 엔티티에 정적 팩토리 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/board/entity/BoardCategory.java`

현재 `BoardCategory`는 `@SuperBuilder`를 사용합니다. 아래 정적 팩토리를 추가합니다:

```java
public static BoardCategory of(Board board, BoardCategoryCreateReqDTO req) {
    return BoardCategory.builder()
            .board(board)
            .name(req.name())
            .slug(req.slug())
            .build();
}
```

import도 추가:
```java
import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
```

---

### Step 4: Service 만들기

`BoardService.java`를 참고해서 동일한 구조로 작성합니다.

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/board/service/BoardCategoryService.java`

```java
package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.repository.BoardCategoryRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryService {

    private final BoardCategoryRepository boardCategoryRepository;

    /** 게시판 카테고리 생성 */
    @Transactional
    @LogEvent(value = "board.category.create", message = "카테고리 생성 성공")
    public BoardCategoryResDTO createCategory(Board board, BoardCategoryCreateReqDTO req) {
        BoardCategory category = BoardCategory.of(board, req);
        BoardCategory saved = boardCategoryRepository.save(category);
        return BoardCategoryResDTO.from(saved);
    }
}
```

> **왜 `Board` 엔티티를 파라미터로 받나요?**  
> Service는 Repository만 직접 다룹니다. Board 엔티티 조회는 Usecase 또는 상위 레이어에서 처리해서 넘겨줍니다.

---

### Step 5: Usecase에 메서드 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/board/usecase/BoardUsecase.java`

기존 파일에 의존성과 메서드를 추가합니다:

```java
// 기존 import 및 필드에 추가
private final BoardGetService boardGetService;
private final BoardCategoryService boardCategoryService;

/** 게시판 카테고리 생성 */
@Transactional
public BoardCategoryResDTO createCategory(Long boardId, BoardCategoryCreateReqDTO req) {
    Board board = boardGetService.getBoard(boardId);  // Board 존재 여부 검증
    return boardCategoryService.createCategory(board, req);
}
```

> **왜 Usecase에서 Board를 조회하나요?**  
> Usecase는 여러 Service를 조합하는 역할을 합니다. "boardId로 Board를 찾아서 → 카테고리를 생성한다" 라는 복합 로직을 여기서 처리합니다.  
> Service끼리는 서로 직접 호출하지 않습니다.

---

### Step 6: ResponseMessage에 카테고리 메시지 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/board/controller/ResponseMessage.java`

```java
CATEGORY_CREATED("[카테고리]가 성공적으로 생성되었습니다."),
```

---

### Step 7: Controller 만들기

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/board/controller/BoardCategoryCreateController.java`

```java
package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.usecase.BoardUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.CATEGORY_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardCategoryCreateController {

    private final BoardUsecase boardUsecase;

    @Operation(summary = "게시판 카테고리 생성", description = "특정 게시판에 하위 카테고리를 생성합니다.")
    @PostMapping("/v1/admin/boards/{boardId}/categories")
    public ApiResponse<BoardCategoryResDTO> createCategory(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardCategoryCreateReqDTO req) {
        BoardCategoryResDTO response = boardUsecase.createCategory(boardId, req);
        return ApiResponse.response(HttpStatus.CREATED, CATEGORY_CREATED.getMessage(), response);
    }
}
```

> **`@PathVariable Long boardId`**: URL의 `{boardId}` 부분을 자동으로 바인딩합니다.

---

## 6. 최종 흐름 정리

```
POST /v1/admin/boards/{boardId}/categories
            ↓
BoardCategoryCreateController.createCategory(boardId, req)
            ↓
BoardUsecase.createCategory(boardId, req)
    ├── boardGetService.getBoard(boardId)     ← Board 존재 검증 + 엔티티 조회
    └── boardCategoryService.createCategory(board, req)
                ↓
            BoardCategory.of(board, req)      ← 엔티티 생성
            boardCategoryRepository.save()    ← DB 저장
            BoardCategoryResDTO.from(saved)   ← DTO 변환
```

---

## 7. 구현 순서 체크리스트

```
[ ] 1. BoardType.java        → GENERAL 추가
[ ] 2. Board.java            → isGeneral() 메서드 추가
[ ] 3. ErrorMessage.java     → BOARD_WRITE_NOT_ALLOWED 추가
[ ] 4. BoardWriteNotAllowedException.java → 새 파일 생성
[ ] 5. PostCreateService.java → NOTICE 게시판 접근 제한 로직 추가

[ ] 6. BoardCategoryCreateReqDTO.java  → 새 파일 생성
[ ] 7. BoardCategoryResDTO.java        → 새 파일 생성
[ ] 8. BoardCategory.java              → of() 정적 팩토리 추가
[ ] 9. BoardCategoryService.java       → 새 파일 생성
[  ] 10. BoardUsecase.java              → createCategory() 메서드 추가
[ ] 11. ResponseMessage.java           → CATEGORY_CREATED 추가
[ ] 12. BoardCategoryCreateController.java → 새 파일 생성
```

---

## 8. 주요 참고 파일

| 참고 목적 | 파일 경로 |
|-----------|-----------|
| Board 생성 흐름 전체 | `domain/board/controller/BoardCreateController.java` |
| Usecase 패턴 | `domain/board/usecase/BoardUsecase.java` |
| Service 패턴 | `domain/board/service/BoardService.java` |
| GetService 패턴 | `domain/board/service/BoardGetService.java` |
| 엔티티 팩토리 패턴 | `domain/board/entity/Board.java` |
| DTO 팩토리 패턴 | `domain/board/dto/response/BoardResDTO.java` |
| 예외 처리 패턴 | `domain/board/exception/BoardNotFoundException.java` |
| 에러 메시지 Enum | `domain/board/exception/ErrorMessage.java` |
| 응답 메시지 Enum | `domain/board/controller/ResponseMessage.java` |
| 타 도메인에서 GetService 사용 예시 | `domain/post/service/post/PostCreateService.java` |

---

## 9. 배경 — 왜 기존에 Category 생성 API가 없었나?

지금까지 BoardCategory(하위 게시판)는 **SQL을 직접 DB에 실행해서** 추가했습니다.

```sql
INSERT INTO board_category (board_id, name, slug, created_at, updated_at)
VALUES (1, '제휴', 'partnership', NOW(), NOW());
```

초기 서비스 세팅 시 게시판 구조가 고정되어 있었기 때문에, 별도 API 없이 배포 시 마이그레이션 SQL로 처리했습니다.  
이번에 GENERAL 타입 게시판이 추가되면서 **관리자가 동적으로 카테고리를 만들 수 있어야** 하므로, 위에서 설명한 API를 새로 구현합니다.

---

## 10. Post 도메인 — 첨부파일 필드 추가

현재 게시글에는 이미지만 첨부할 수 있습니다. PDF, PPT 등 **일반 파일도 첨부**할 수 있도록 필드를 추가해야 합니다.

### 현재 이미지 구조 파악

이미지가 어떻게 저장되는지 먼저 이해하고 나서 동일한 패턴으로 파일을 추가합니다.

```
PostCreateReqDTO
  └── List<PostImageCreateReqDTO>  (originalUrl, sequence)
            ↓ PostCreateService에서 처리
        PostImageUrl 엔티티  (post_id, originalUrl, sequence)
            ↓ PostImageUrlRepository
        post_image_url 테이블
```

관련 파일:
- `domain/post/dto/request/PostImageCreateReqDTO.java`
- `domain/post/entity/PostImageUrl.java`
- `domain/post/service/image/PostImageCreateService.java`
- `domain/post/repository/PostImageUrlRepository.java`

### 추가해야 할 파일 구조

```
domain/post/
├── dto/
│   ├── request/
│   │   └── PostFileCreateReqDTO.java     ← 새로 생성
│   └── response/
│       └── PostFileResDTO.java           ← 새로 생성
├── entity/
│   └── PostFileUrl.java                  ← 새로 생성
├── repository/
│   └── PostFileUrlRepository.java        ← 새로 생성
└── service/
    └── image/
        └── PostFileCreateService.java    ← 새로 생성
```

기존 파일 수정 대상:
- `PostCreateReqDTO.java` → `fileList` 필드 추가
- `PostCreateService.java` → 파일 저장 로직 추가
- `PostDetailResDTO.java` → `fileList` 필드 추가

---

### Step 1: Request DTO 생성

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/post/dto/request/PostFileCreateReqDTO.java`

`PostImageCreateReqDTO`와 동일한 패턴으로 작성합니다.  
참고 파일: `domain/post/dto/request/PostImageCreateReqDTO.java`

```java
package com.tavemakers.surf.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PostFileCreateReqDTO(

        @Schema(description = "파일 S3 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/files/doc.pdf")
        @NotBlank
        String fileUrl,

        @Schema(description = "원본 파일명", example = "2025_발표자료.pdf")
        @NotBlank
        String originalFileName
) {}
```

---

### Step 2: Response DTO 생성

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/post/dto/response/PostFileResDTO.java`

```java
package com.tavemakers.surf.domain.post.dto.response;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import io.swagger.v3.oas.annotations.media.Schema;

public record PostFileResDTO(

        @Schema(description = "파일 ID")
        Long id,

        @Schema(description = "파일 S3 URL")
        String fileUrl,

        @Schema(description = "원본 파일명")
        String originalFileName
) {
    public static PostFileResDTO from(PostFileUrl file) {
        return new PostFileResDTO(
                file.getId(),
                file.getFileUrl(),
                file.getOriginalFileName()
        );
    }
}
```

---

### Step 3: 엔티티 생성

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/post/entity/PostFileUrl.java`

`PostImageUrl.java`와 동일한 구조입니다.  
참고 파일: `domain/post/entity/PostImageUrl.java`

```java
package com.tavemakers.surf.domain.post.entity;

import com.tavemakers.surf.domain.post.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFileUrl extends BaseEntity {

    @Id @Tsid
    @Column(name = "post_file_url_id")
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public static PostFileUrl of(Post post, PostFileCreateReqDTO dto) {
        return PostFileUrl.builder()
                .post(post)
                .fileUrl(dto.fileUrl())
                .originalFileName(dto.originalFileName())
                .build();
    }
}
```

---

### Step 4: Repository 생성

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/post/repository/PostFileUrlRepository.java`

```java
package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostFileUrlRepository extends JpaRepository<PostFileUrl, Long> {
    List<PostFileUrl> findAllByPostId(Long postId);
}
```

---

### Step 5: Service 생성

**파일 생성**: `src/main/java/com/tavemakers/surf/domain/post/service/image/PostFileCreateService.java`

`PostImageCreateService.java`와 동일한 구조입니다.  
참고 파일: `domain/post/service/image/PostImageCreateService.java`

```java
package com.tavemakers.surf.domain.post.service.image;

import com.tavemakers.surf.domain.post.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostFileResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFileCreateService {

    private final PostFileUrlRepository repository;

    /** 게시글 첨부파일 일괄 저장 */
    @Transactional
    public List<PostFileResDTO> saveAll(Post post, List<PostFileCreateReqDTO> dto) {
        if (dto == null || dto.isEmpty()) {
            return List.of();
        }

        List<PostFileUrl> files = dto.stream()
                .map(f -> PostFileUrl.of(post, f))
                .toList();
        return repository.saveAll(files).stream()
                .map(PostFileResDTO::from)
                .toList();
    }
}
```

---

### Step 6: PostCreateReqDTO에 fileList 필드 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/post/dto/request/PostCreateReqDTO.java`

`imageUrlList` 아래에 추가합니다:

```java
@Schema(description = "게시글 첨부파일 목록")
List<PostFileCreateReqDTO> fileList,
```

그리고 편의 메서드도 추가합니다:

```java
public boolean hasFile() {
    return fileList != null && !fileList.isEmpty();
}
```

---

### Step 7: PostCreateService에 파일 저장 로직 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/post/service/post/PostCreateService.java`

의존성 주입 추가:
```java
private final PostFileCreateService fileCreateService;
```

`createPost()` 메서드 안, 이미지 저장 블록(`if (req.hasImage())`) 바로 아래에 추가:

```java
List<PostFileResDTO> fileResponseList = null;
if (req.hasFile()) {
    fileResponseList = fileCreateService.saveAll(saved, req.fileList());
}
```

그리고 `PostDetailResDTO.of()` 호출부에 `fileResponseList`를 넘겨야 합니다 (아래 Step 8 참고).

---

### Step 8: PostDetailResDTO에 fileList 필드 추가

**파일**: `src/main/java/com/tavemakers/surf/domain/post/dto/response/PostDetailResDTO.java`

필드 추가:
```java
@Schema(description = "게시글 첨부파일 목록")
List<PostFileResDTO> fileList,
```

`of()` 팩토리 메서드 시그니처와 빌더에도 `fileList`를 추가합니다:

```java
public static PostDetailResDTO of(
        Post post,
        boolean scrappedByMe,
        boolean likedByMe,
        boolean isMine,
        List<PostImageResDTO> imageUrlList,
        List<PostFileResDTO> fileList,   // ← 추가
        LocalDateTime reservedAt,
        int viewCount
) {
    return PostDetailResDTO.builder()
            // ... 기존 필드들 ...
            .fileList(fileList)          // ← 추가
            .build();
}
```

> **주의**: `PostDetailResDTO.of()`를 호출하는 곳이 여러 군데 있을 수 있습니다.  
> `PostCreateService`, `PostGetService`, `PostPatchService` 등을 검색해서 모두 수정해야 합니다.  
> 파악 방법: IDE에서 `PostDetailResDTO.of(` 를 전체 검색하세요.

---

## 11. 게시글 예약 발행 흐름 이해

게시글을 특정 시간에 자동으로 발행하는 기능입니다.  
요청 시 `reservedAt` 필드에 미래 시간을 넣으면 해당 시간에 자동으로 게시됩니다.

### 11-1. 전체 흐름 다이어그램

```
POST /v1/user/posts  (reservedAt 포함)
            ↓
PostCreateController
            ↓
PostCreateUsecase.createPost()
    ├── PostCreateService.createPost()
    │       ↓
    │   Post 엔티티 생성 (isReserved = true)
    │   DB 저장 (아직 발행 안 됨 — 피드에 안 보임)
    │   PostPublishedEvent 미발행 (예약이므로 건너뜀)
    │
    └── (req.isReserved() == true이면)
        ReservationUsecase.reservePost(postId, reservedAt)
                ↓
            Reservation 엔티티 생성 (status = RESERVED)
            DB 저장
            ReservationScheduleService.schedule()
                ↓
            Spring TaskScheduler에 미래 시간 등록

[지정한 시간 도달]
            ↓
PostPublishTask.run()
            ↓
PostPublishRunner.publishPost(reservationId)
    ├── Reservation 조회
    ├── Post 조회
    ├── post.publish()         → isReserved = false, postedAt = 지금
    ├── reservation.publish()  → status = PUBLISHED
    └── PostPublishedEvent 발행 → 알림, 피드 처리 등
```

---

### 11-2. 관련 파일 설명

| 파일 | 역할 |
|------|------|
| `domain/post/service/post/PostCreateUsecase.java` | 게시글 생성 + 예약 등록 총괄 |
| `domain/post/service/post/PostCreateService.java` | Post 엔티티 생성 및 저장 |
| `domain/reservation/usecase/ReservationUsecase.java` | 예약 등록/수정/조회 총괄 |
| `domain/reservation/service/ReservationCreateService.java` | Reservation 엔티티 저장 |
| `domain/reservation/service/ReservationScheduleService.java` | Spring TaskScheduler에 작업 등록 |
| `domain/reservation/task/PostPublishTask.java` | 예약 시간에 실행될 Runnable 태스크 |
| `domain/reservation/task/PostPublishRunner.java` | 실제 발행 처리 (Post.publish, 이벤트 발행) |
| `domain/reservation/entity/Reservation.java` | 예약 정보 엔티티 |
| `domain/reservation/entity/ReservationStatus.java` | RESERVED / PUBLISHED / CANCELLED |

---

### 11-3. 코드로 이해하기

**PostCreateUsecase** [`domain/post/service/post/PostCreateUsecase.java`]
```java
@Transactional
public PostDetailResDTO createPost(PostCreateReqDTO req, Long memberId) {
    PostDetailResDTO result = postCreateService.createPost(req, memberId);

    if (req.isReserved()) {
        // reservedAt이 null이 아니면 예약으로 처리
        reservationUsecase.reservePost(result.postId(), req.reservedAt());
    }
    return result;
}
```

**ReservationUsecase** [`domain/reservation/usecase/ReservationUsecase.java`]
```java
public void reservePost(Long postId, LocalDateTime reservedAt) {
    Instant publishAt = toInstant(reservedAt);               // LocalDateTime → Instant 변환
    Reservation reservation = Reservation.of(postId, publishAt);
    Reservation saved = reservationCreateService.save(reservation);
    scheduleService.schedule(saved.getId(), publishAt);      // Spring Scheduler에 등록
}
```

**PostPublishRunner** [`domain/reservation/task/PostPublishRunner.java`]
```java
@Transactional
public void publishPost(Long reservationId) {
    Reservation reservation = reservationGetService.getReservationById(reservationId);
    Post post = getPost(reservation);

    if (post == null) {
        reservation.cancel();   // 게시글이 삭제된 경우 → 예약 취소
        return;
    }

    post.publish();             // isReserved = false, postedAt = now()
    reservation.publish();      // status = PUBLISHED
    eventPublisher.publishEvent(new PostPublishedEvent(post.getId()));
}
```

---

### 11-4. 예약 관련 주요 규칙

- `reservedAt`이 `null`이면 → 즉시 발행 (일반 게시글)
- `reservedAt`이 미래 시간이면 → 예약 발행
- 예약 게시글은 `isReserved = true` 상태로 저장되며, 이 상태에선 **피드에 노출되지 않습니다**
- 예약 시간 변경: `ReservationUsecase.updateReservationPost()` 호출 → 기존 예약 CANCELLED 처리 후 새로 등록
- 서버 재시작 주의: Spring TaskScheduler는 **인메모리** 기반이라 서버가 재시작되면 예약이 사라집니다. (현재 알려진 한계)

---

## 12. 구현 전체 체크리스트 (최종)

### Task 1: BoardType.GENERAL 추가 + 권한 분리
```
[ ] BoardType.java                      → GENERAL 추가
[ ] Board.java                          → isGeneral() 메서드 추가
[ ] ErrorMessage.java (board)           → BOARD_WRITE_NOT_ALLOWED 추가
[ ] BoardWriteNotAllowedException.java  → 새 파일 생성
[ ] PostCreateService.java              → NOTICE 게시판 작성 제한 로직 추가
```

### Task 2: BoardCategory 생성 API
```
[ ] BoardCategoryCreateReqDTO.java      → 새 파일 생성
[ ] BoardCategoryResDTO.java            → 새 파일 생성
[ ] BoardCategory.java                  → of() 정적 팩토리 추가
[ ] BoardCategoryService.java           → 새 파일 생성
[ ] BoardUsecase.java                   → createCategory() 메서드 추가
[ ] ResponseMessage.java                → CATEGORY_CREATED 추가
[ ] BoardCategoryCreateController.java  → 새 파일 생성
```

### Task 3: 게시글 첨부파일 추가
```
[ ] PostFileCreateReqDTO.java           → 새 파일 생성
[ ] PostFileResDTO.java                 → 새 파일 생성
[ ] PostFileUrl.java                    → 새 파일 생성 (엔티티)
[ ] PostFileUrlRepository.java          → 새 파일 생성
[ ] PostFileCreateService.java          → 새 파일 생성
[ ] PostCreateReqDTO.java               → fileList 필드 + hasFile() 메서드 추가
[ ] PostCreateService.java              → 파일 저장 로직 추가
[ ] PostDetailResDTO.java               → fileList 필드 + of() 시그니처 수정
[ ] PostDetailResDTO.of() 호출부 전체   → fileList 파라미터 추가 (IDE 전체 검색 필요)
```
