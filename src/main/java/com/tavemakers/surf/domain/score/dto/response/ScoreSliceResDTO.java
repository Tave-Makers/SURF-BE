package com.tavemakers.surf.domain.score.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record ScoreSliceResDTO(
        List<ScoreDetailResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    public static ScoreSliceResDTO from(Slice<ScoreDetailResDTO> slice) {
        return ScoreSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
