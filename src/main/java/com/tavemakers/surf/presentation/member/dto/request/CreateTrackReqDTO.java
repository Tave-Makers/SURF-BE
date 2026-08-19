package com.tavemakers.surf.presentation.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.Part;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateTrackReqDTO(
        @NotNull
        @Schema(description = "기수", example = "19")
        Integer generation,

        @NotNull
        @Schema(description = "파트", example = "BACKEND")
        Part part
) {
}
