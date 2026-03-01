package com.tavemakers.surf.domain.badge.dto.response;

import org.springframework.data.domain.Slice;

import java.util.List;

public record MemberBadgeSliceResDTO(
        List<MemberBadgeResDTO> content,
        int pageNumber,
        int pageSize,
        boolean hasNext
) {

    public static MemberBadgeSliceResDTO from(Slice<MemberBadgeResDTO> slice) {
        return new MemberBadgeSliceResDTO(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}