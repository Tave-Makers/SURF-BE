package com.tavemakers.surf.domain.member.dto.response;

import lombok.Builder;

@Builder
public record GenerationResDTO(
        int generation,
        String name
) {
    public static final String GENERATION_UI_SUFFIX = "기";

    public static GenerationResDTO from(Integer generation) {
        return GenerationResDTO.builder()
                .generation(generation)
                .name(generation + GENERATION_UI_SUFFIX)
                .build();
    }
}
