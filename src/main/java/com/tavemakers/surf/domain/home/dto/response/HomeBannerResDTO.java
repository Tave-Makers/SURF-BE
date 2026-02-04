package com.tavemakers.surf.domain.home.dto.response;

import com.tavemakers.surf.domain.home.entity.HomeBanner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record HomeBannerResDTO(

        @Schema(description = "배너 ID", example = "1")
        Long id,

        @Schema(description = "배너 이름", example = "새로운 17기 환영 배너")
        String name,

        @Schema(description = "배너 이미지 URL", example = "https://example.com/banner1.jpg")
        String imageUrl,

        @Schema(description = "배너 링크 URL", example = "https://www.example.com/promotion")
        String linkUrl,

        @Schema(description = "배너 노출 순서", example = "1")
        Integer displayOrder,

        @Schema(description = "배너 활성 상태", example = "true")
        boolean status
) {
    public static HomeBannerResDTO from(HomeBanner b) {
        return HomeBannerResDTO.builder()
                .id(b.getId())
                .name(b.getName())
                .imageUrl(b.getImageUrl())
                .linkUrl(b.getLinkUrl())
                .displayOrder(b.getDisplayOrder())
                .status(b.isStatus())
                .build();
    }
}