package com.tavemakers.surf.presentation.notification.dto.response;

import org.springframework.data.domain.Slice;

import java.util.List;

public record NotificationSliceResDTO(
        List<NotificationResDTO> content,
        int pageNumber,
        int pageSize,
        boolean hasNext
) {

    /** Slice 조회 결과를 무한스크롤 응답 형태로 변환 */
    public static NotificationSliceResDTO from(Slice<NotificationResDTO> slice) {
        return new NotificationSliceResDTO(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
