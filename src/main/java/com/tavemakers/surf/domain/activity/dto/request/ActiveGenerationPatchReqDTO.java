package com.tavemakers.surf.domain.activity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActiveGenerationPatchReqDTO(
        @NotNull
        @Min(1)
        Integer activeGeneration
) {}
