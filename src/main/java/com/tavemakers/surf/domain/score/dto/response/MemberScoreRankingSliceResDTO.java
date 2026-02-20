package com.tavemakers.surf.domain.score.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record MemberScoreRankingSliceResDTO(
        List<MemberScoreRankingResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    /** Slice로부터 래퍼 DTO 생성 */
    public static MemberScoreRankingSliceResDTO from(Slice<MemberScoreRankingResDTO> slice) {
        return MemberScoreRankingSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
