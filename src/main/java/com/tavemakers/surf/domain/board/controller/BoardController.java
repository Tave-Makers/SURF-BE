package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.facade.BoardFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardController {
    private final BoardFacade boardFacade;

    @Operation(summary = "게시판 생성", description = "새로운 게시판을 생성합니다.")
    @PostMapping("/v1/admin/boards")
    public ApiResponse<BoardResDTO> createBoard(
            @Valid @RequestBody BoardCreateReqDTO req) {
        BoardResDTO response = boardFacade.createBoard(req);
        return ApiResponse.response(HttpStatus.CREATED, BOARD_CREATED.getMessage(), response);
    }

    @Operation(summary = "게시판 목록 조회", description = "모든 게시판을 조회합니다.")
    @GetMapping("/v1/user/boards")
    public ApiResponse<List<BoardResDTO>> getBoards() {
        List<BoardResDTO> response = boardFacade.getBoards();
        return ApiResponse.response(HttpStatus.OK, BOARD_READ.getMessage(), response);
    }

    @Operation(summary = "게시판 조회", description = "특정 ID의 게시판을 조회합니다.")
    @GetMapping("/v1/admin/boards/{boardId}")
    public ApiResponse<BoardResDTO> getBoard(
            @PathVariable Long boardId) {
        BoardResDTO response = boardFacade.getBoard(boardId);
        return ApiResponse.response(HttpStatus.OK, BOARD_READ.getMessage(), response);
    }

    @Operation(summary = "게시판 수정", description = "특정 ID의 게시판을 수정합니다.")
    @PatchMapping("/v1/admin/boards/{boardId}")
    public ApiResponse<BoardResDTO> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardUpdateReqDTO req) {
        BoardResDTO response = boardFacade.updateBoard(boardId, req);
        return ApiResponse.response(HttpStatus.OK, BOARD_UPDATED.getMessage(), response);
    }

    @Operation(summary = "게시판 삭제", description = "특정 ID의 게시판을 삭제합니다.")
    @DeleteMapping("/v1/admin/boards/{boardId}")
    public ApiResponse<Void> deleteBoard(
            @PathVariable Long boardId) {
        boardFacade.deleteBoard(boardId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, BOARD_DELETED.getMessage());
    }
}
