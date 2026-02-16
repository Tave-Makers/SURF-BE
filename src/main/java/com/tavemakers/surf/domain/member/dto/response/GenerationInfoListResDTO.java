package com.tavemakers.surf.domain.member.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GenerationInfoListResDTO(
        List<GenerationResDTO> generations
) {
    public static GenerationInfoListResDTO from(List<Integer> generations) {
        List<GenerationResDTO> generationResDTOList = generations.stream()
                .map(GenerationResDTO::from)
                .toList();

        return GenerationInfoListResDTO.builder()
                .generations(generationResDTOList)
                .build();
    }
}
