package com.tavemakers.surf.presentation.board.dto.response;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "게시판별 카테고리 목록 응답 DTO")
public record BoardCategoriesGetResDTO(

        @Schema(description = "게시판 ID", example = "1")
        Long boardId,

        @Schema(description = "게시판 이름", example = "공지사항")
        String boardName,

        @Schema(description = "게시판 타입", example = "NOTICE")
        BoardType boardType,

        @Schema(description = "게시판에 속한 카테고리 목록")
        List<BoardCategoryResDTO> categories
) {
    public static BoardCategoriesGetResDTO of(Board board, List<BoardCategory> categories) {
        return new BoardCategoriesGetResDTO(
                board.getId(),
                board.getName(),
                board.getType(),
                categories.stream()
                        .map(BoardCategoryResDTO::from)
                        .toList()
        );
    }
}
