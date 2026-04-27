package com.tavemakers.surf.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게시판 카테고리 생성 요청 DTO")
public record BoardCategoryCreateReqDTO(

        @Schema(description = "카테고리 이름", example = "제휴")
        @NotBlank String name,

        @Schema(description = "URL용 식별 슬러그 (게시판 내 unique)", example = "partnership")
        @NotBlank String slug
) {
}
