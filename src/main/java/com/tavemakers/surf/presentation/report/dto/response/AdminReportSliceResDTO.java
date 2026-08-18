package com.tavemakers.surf.presentation.report.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record AdminReportSliceResDTO(
        List<AdminReportListItemResDTO> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        @JsonProperty("isLast") boolean isLast
) {
    /** Slice 래퍼를 생성한다. */
    public static AdminReportSliceResDTO from(Slice<AdminReportListItemResDTO> slice) {
        return AdminReportSliceResDTO.builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .numberOfElements(slice.getNumberOfElements())
                .isLast(slice.isLast())
                .build();
    }
}
