package com.tavemakers.surf.domain.schedule.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "월별 일정 조회")
public record ScheduleMonthlyResDTO(
        @Schema(description = "조회 연도", example = "2025")
        int year,

        @Schema(description = "조회 월", example = "11")
        int month,

        List<ScheduleResDTO> scheduleResDTOList
) {
    public static ScheduleMonthlyResDTO of(int year, int month, List<ScheduleResDTO> scheduleResDTOList) {
        return new ScheduleMonthlyResDTO(year, month, scheduleResDTOList);
    }
}
