package com.tavemakers.surf.domain.activity.dto.response;

public record ActiveGenerationResDTO(
        Integer activeGeneration
) {
    public static ActiveGenerationResDTO of(Integer generation) {
        return new ActiveGenerationResDTO(generation);
    }
}
