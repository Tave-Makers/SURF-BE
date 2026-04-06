package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.usecase.BoardUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardGetController {
    private final BoardUsecase boardUsecase;

    @Operation(summary = "게시판 목록 조회", description = "모든 게시판을 조회합니다.")
    @GetMapping("/v1/user/boards")
    public ApiResponse<List<BoardResDTO>> getBoards() {
        List<BoardResDTO> response = boardUsecase.getBoards();
        return ApiResponse.response(HttpStatus.OK, BOARD_READ.getMessage(), response);
    }

    @Operation(summary = "게시판 조회", description = "특정 ID의 게시판을 조회합니다.")
    @GetMapping("/v1/admin/boards/{boardId}")
    public ApiResponse<BoardResDTO> getBoard(
            @PathVariable Long boardId) {
        BoardResDTO response = boardUsecase.getBoard(boardId);
        return ApiResponse.response(HttpStatus.OK, BOARD_READ.getMessage(), response);
    }
}
