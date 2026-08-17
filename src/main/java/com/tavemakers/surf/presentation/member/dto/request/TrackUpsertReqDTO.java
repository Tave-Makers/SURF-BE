package com.tavemakers.surf.presentation.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.Part;
import io.swagger.v3.oas.annotations.media.Schema;

public record TrackUpsertReqDTO(
        @Schema(description = "기수", example = "19")
        Integer generation,

        @Schema(description = "파트", example = "BACKEND")
        Part part
) {
}
