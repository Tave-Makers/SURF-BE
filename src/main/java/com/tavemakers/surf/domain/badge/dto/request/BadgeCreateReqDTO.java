package com.tavemakers.surf.domain.badge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BadgeCreateReqDTO {

    @Schema(description = "배지 이름", example = "16기 우수 스터디(1위)")
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "배지 이미지 URL", example = "https://example.png")
    @NotBlank
    @Size(max = 255)
    private String imageUrl;

    @Schema(description = "배지 설명", example = "16기 전반기 동안 탁월한 활동을 보여준 팀에게 수여되는 배지입니다.")
    @NotBlank
    private String description;

    @Schema(description = "배지 획득 조건", example = "전반기 스터디 평가 1위")
    @NotBlank
    @Size(max = 255)
    private String requirement;
}