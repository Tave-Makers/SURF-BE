package com.tavemakers.surf.domain.badge.dto.response;

import org.springframework.data.domain.Slice;

import java.util.List;

public record BadgeSliceResDTO(
        List<BadgeResDTO> content,
        int pageNumber,
        int pageSize,
        boolean hasNext
) {

    public static BadgeSliceResDTO from(Slice<BadgeResDTO> slice) {
        return new BadgeSliceResDTO(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}