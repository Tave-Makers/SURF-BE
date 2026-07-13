package com.tavemakers.surf.presentation.activity.dto.activeGeneration.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActiveGenerationUpdateReqDTO(
        @NotNull
        @Min(1)
        Integer activeGeneration
) {}
