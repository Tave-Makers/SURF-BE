package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record ActivityRecordSliceResDTO(
        List<ActivityRecordResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean isLast
) {
    public static ActivityRecordSliceResDTO from(Slice<ActivityRecordResDTO> slice) {
        return ActivityRecordSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
