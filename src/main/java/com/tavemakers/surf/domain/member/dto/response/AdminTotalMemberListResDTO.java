package com.tavemakers.surf.domain.member.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminTotalMemberListResDTO(
        long totalMemberCount,
        List<GenerationResDTO> generations
) {
    public static AdminTotalMemberListResDTO of(long totalMemberCount, List<Integer> generations) {
        List<GenerationResDTO> generationResDTOList = generations.stream()
                .map(GenerationResDTO::from)
                .toList();

        return AdminTotalMemberListResDTO.builder()
                .totalMemberCount(totalMemberCount)
                .generations(generationResDTOList)
                .build();
    }
}
