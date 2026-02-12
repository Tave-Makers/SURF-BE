package com.tavemakers.surf.domain.member.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record MemberRegistrationSliceResDTO(
        List<MemberRegistrationDetailResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    public static MemberRegistrationSliceResDTO from(Slice<MemberRegistrationDetailResDTO> slice) {
        return MemberRegistrationSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
