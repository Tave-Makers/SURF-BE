package com.tavemakers.surf.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostFileCreateReqDTO(

        @Schema(description = "파일 S3 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/files/doc.pdf")
        @NotBlank
        String fileUrl,

        @Schema(description = "원본 파일명", example = "2025_발표자료.pdf")
        @NotBlank
        String originalFileName,

        @Schema(description = "파일 게시 순서", example = "1")
        @NotNull
        Integer sequence
) {
}
