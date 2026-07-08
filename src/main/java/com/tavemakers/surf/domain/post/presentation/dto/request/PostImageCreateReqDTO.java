package com.tavemakers.surf.domain.post.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostImageCreateReqDTO(

        @Schema(description = "이미지 원본 S3 URL", example = "meeting.jpg")
        @NotBlank
        String originalUrl,

        @Schema(description = "이미지 게시 순서", example = "1")
        @NotNull
        Integer sequence

) {
}
