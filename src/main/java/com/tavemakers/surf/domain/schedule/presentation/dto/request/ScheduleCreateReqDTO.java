package com.tavemakers.surf.domain.schedule.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "일정 생성")
public record ScheduleCreateReqDTO(
        @Schema(description = "일정 카테고리", example = "정규행사")
        @NotBlank
        String category,

        @Schema(description = "일정 제목", example = "만남의 장")
        @NotBlank
        String title,

        @Schema(description = "일정 시작 시간", example = "2025-11-15T14:00:00")
        @NotNull
        LocalDateTime startAt,

        @Schema(description = "일정 종료 시간", example = "2025-11-15T16:00:00")
        @NotNull
        LocalDateTime endAt,

        @Schema(description = "일정 장소", example = "세종대학교 광개토대왕관")
        @NotBlank
        String location
) {}
