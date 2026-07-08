package com.tavemakers.surf.domain.board.presentation.controller;

import com.tavemakers.surf.domain.board.application.usecase.BoardCategoryUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.board.presentation.controller.ResponseMessage.BOARD_CATEGORY_DELETED;

@RestController
@RequiredArgsConstructor
@RequestMapping
// 게시판 태그와 동일한 태그 사용
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardCategoryDeleteController {

    private final BoardCategoryUsecase boardCategoryUsecase;

    /** 게시판 카테고리를 삭제합니다. (하위 게시글이 없는 경우에만 가능) */
    @Operation(summary = "게시판 카테고리 삭제", description = "특정 게시판의 카테고리를 삭제합니다. 하위 게시글이 존재하면 삭제할 수 없습니다.")
    @DeleteMapping("/v1/admin/boards/{boardId}/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @PathVariable Long boardId,
            @PathVariable Long categoryId) {
        boardCategoryUsecase.deleteCategory(boardId, categoryId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, BOARD_CATEGORY_DELETED.getMessage());
    }

}
