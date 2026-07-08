package com.tavemakers.surf.domain.home.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record HomeBannerCreateReqDTO(

        @Schema(description = "배너 이름", example = "새로운 17기 환영 배너")
        @NotBlank
        String name,

        @Schema(description = "배너 이미지 URL", example = "https://example-bucket.s3.amazonaws.com/banner1.png")
        @NotBlank
        String imageUrl,   // S3 업로드 완료 후 URL

        @Schema(description = "배너 링크 URL", example = "https://www.example.com/promotion")
        @NotBlank
        String linkUrl
) {

}