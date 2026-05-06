package com.tavemakers.surf.domain.badge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BadgeUpdateReqDTO {

    @Schema(description = "수정할 배지 이름", example = "16기 우수회원 (1위)")
    @Size(max = 100)
    private String name;

    @Schema(description = "수정할 배지 이미지 URL", example = "https://example.png")
    @Size(max = 255)
    private String imageUrl;

    @Schema(description = "수정할 배지 설명", example = "한 기수 동안 우수한 활동을 한 회원에게 수여되는 배지입니다.")
    private String description;

    @Schema(description = "수정할 배지 획득 조건", example = "활동점수 1위")
    @Size(max = 255)
    private String requirement;
}