package com.tavemakers.surf.domain.home.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HomeBannerReorderReqDTO(

        @Schema(description = "전체 배너 ID 목록", example = "[3, 1, 2]")
        @NotNull
        List<@NotNull Long> orderedIds
) {
}