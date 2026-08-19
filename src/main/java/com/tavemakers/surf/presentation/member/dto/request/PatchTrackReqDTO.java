package com.tavemakers.surf.presentation.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.Part;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

public record PatchTrackReqDTO(
        @Schema(description = "기수", example = "19")
        Integer generation,

        @Schema(description = "파트", example = "BACKEND")
        Part part
) {
    @AssertTrue(message = "generation 또는 part 중 하나는 반드시 포함되어야 합니다.")
    public boolean hasAtLeastOneField() {
        return generation != null || part != null;
    }
}
