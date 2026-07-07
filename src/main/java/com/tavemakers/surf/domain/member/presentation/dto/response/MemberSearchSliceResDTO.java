package com.tavemakers.surf.domain.member.presentation.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record MemberSearchSliceResDTO(
        List<MemberSearchDetailResDTO> content,
        Long totalCount,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    public static MemberSearchSliceResDTO of(Slice<MemberSearchDetailResDTO> slice, Long totalCount) {
        return MemberSearchSliceResDTO.builder()
                .content(slice.getContent())
                .totalCount(totalCount)
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }

}
