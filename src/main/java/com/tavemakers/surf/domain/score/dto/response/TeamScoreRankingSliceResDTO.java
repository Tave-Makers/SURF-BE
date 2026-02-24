package com.tavemakers.surf.domain.score.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record TeamScoreRankingSliceResDTO(
        List<TeamScoreRankingResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        @JsonProperty("isLast") boolean isLast
) {
    /** Slice로부터 래퍼 DTO 생성 */
    public static TeamScoreRankingSliceResDTO from(Slice<TeamScoreRankingResDTO> slice) {
        return TeamScoreRankingSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
