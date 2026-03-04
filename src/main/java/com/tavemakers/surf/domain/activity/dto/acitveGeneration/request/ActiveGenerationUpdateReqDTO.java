package com.tavemakers.surf.domain.activity.dto.acitveGeneration.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActiveGenerationUpdateReqDTO(
        @NotNull
        @Min(1)
        Integer activeGeneration
) {}
