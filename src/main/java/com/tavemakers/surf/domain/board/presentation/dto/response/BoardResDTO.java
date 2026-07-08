package com.tavemakers.surf.domain.board.presentation.dto.response;

import com.tavemakers.surf.domain.board.domain.entity.Board;
import com.tavemakers.surf.domain.board.domain.entity.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 응답 DTO")
public record BoardResDTO(

        @Schema(description = "게시판 ID", example = "1")
        Long id,

        @Schema(description = "게시판 이름", example = "공지사항")
        String name,

        @Schema(description = "게시판 타입", example = "NOTICE")
        BoardType type
) {
    public static BoardResDTO from(Board board) {
        return new BoardResDTO(
                board.getId(),
                board.getName(),
                board.getType()
        );
    }
}