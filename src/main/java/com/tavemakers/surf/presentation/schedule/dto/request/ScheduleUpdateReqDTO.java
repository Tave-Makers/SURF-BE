package com.tavemakers.surf.presentation.schedule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ScheduleUpdateReqDTO(
        @Schema(description = "일정 카테고리", example = "정규행사")
        String category,

        @Schema(description = "일정 제목", example = "만남의 장")
        String title,

        @Schema(description = "일정 시작 시간", example = "2025-11-15T14:00:00")
        LocalDateTime startAt,

        @Schema(description = "일정 종료 시간", example = "2025-11-15T16:00:00")
        LocalDateTime endAt,

        @Schema(description = "일정 장소", example = "세종대학교 광개토대왕관")
        String location
) {}
