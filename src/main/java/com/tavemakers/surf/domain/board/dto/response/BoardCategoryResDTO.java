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
