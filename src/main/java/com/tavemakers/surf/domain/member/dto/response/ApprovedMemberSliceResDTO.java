package com.tavemakers.surf.domain.member.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record ApprovedMemberSliceResDTO(
        List<MemberRegistrationDetailResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    public static ApprovedMemberSliceResDTO from(Slice<MemberRegistrationDetailResDTO> slice) {
        return ApprovedMemberSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
