package com.tavemakers.surf.domain.board.controller;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.usecase.BoardUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.board.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시판", description = "추후 MVP를 통해 디벨롭 될 예정")
public class BoardCreateController {
    private final BoardUsecase boardUsecase;

    @Operation(summary = "게시판 생성", description = "새로운 게시판을 생성합니다.")
    @PostMapping("/v1/admin/boards")
    public ApiResponse<BoardResDTO> createBoard(
            @Valid @RequestBody BoardCreateReqDTO req) {
        BoardResDTO response = boardUsecase.createBoard(req);
        return ApiResponse.response(HttpStatus.CREATED, BOARD_CREATED.getMessage(), response);
    }
}
