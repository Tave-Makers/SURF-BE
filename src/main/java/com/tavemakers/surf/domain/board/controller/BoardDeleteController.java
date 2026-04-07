package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.usecase.BoardUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardDeleteController {
    private final BoardUsecase boardUsecase;

    @Operation(summary = "게시판 삭제", description = "특정 ID의 게시판을 삭제합니다.")
    @DeleteMapping("/v1/admin/boards/{boardId}")
    public ApiResponse<Void> deleteBoard(
            @PathVariable Long boardId) {
        boardUsecase.deleteBoard(boardId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, BOARD_DELETED.getMessage());
    }
}
