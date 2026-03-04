package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record AdminActivityRecordSliceResDTO(
        List<AdminActivityRecordResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        @JsonProperty("isLast") boolean isLast
) {
    /** Slice 래퍼 생성 */
    public static AdminActivityRecordSliceResDTO from(Slice<AdminActivityRecordResDTO> slice) {
        return AdminActivityRecordSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
