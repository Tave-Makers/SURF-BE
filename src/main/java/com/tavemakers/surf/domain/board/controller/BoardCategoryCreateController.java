package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.usecase.BoardCategoryUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.BOARD_CATEGORY_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
// 게시판 태그와 동일한 태그 사용
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardCategoryCreateController {

    private final BoardCategoryUsecase  boardCategoryUsecase;

    /** 게시판 카테고리를 생성합니다. */
    @Operation(summary = "게시판 카테고리 생성", description = "특정 게시판에 하위 카테고리를 생성합니다.")
    @PostMapping("/v1/admin/boards/{boardId}/categories")
    public ApiResponse<BoardCategoryResDTO> createCategory(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardCategoryCreateReqDTO req) {
        BoardCategoryResDTO response = boardCategoryUsecase.createCategory(boardId, req);
        return ApiResponse.response(HttpStatus.CREATED, BOARD_CATEGORY_CREATED.getMessage(), response);
    }

}
