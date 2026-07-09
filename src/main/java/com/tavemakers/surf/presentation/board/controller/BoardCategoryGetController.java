package com.tavemakers.surf.presentation.board.controller;

import com.tavemakers.surf.presentation.board.dto.response.BoardCategoriesGetResDTO;
import com.tavemakers.surf.application.board.usecase.BoardCategoryUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.presentation.board.controller.ResponseMessage.BOARD_CATEGORY_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
// 게시판 태그와 동일한 태그 사용
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardCategoryGetController {

    private final BoardCategoryUsecase boardCategoryUsecase;

    /** 전체 카테고리를 게시판별로 묶어서 조회합니다. */
    @Operation(summary = "카테고리 목록 게시판별 조회", description = "전체 카테고리를 게시판(Board) 단위로 묶어서 조회합니다.")
    @GetMapping("/v1/user/boards/categories")
    public ApiResponse<List<BoardCategoriesGetResDTO>> getCategoriesGroupedByBoard() {
        List<BoardCategoriesGetResDTO> response = boardCategoryUsecase.getCategoriesGroupedByBoard();
        return ApiResponse.response(HttpStatus.OK, BOARD_CATEGORY_READ.getMessage(), response);
    }

}
