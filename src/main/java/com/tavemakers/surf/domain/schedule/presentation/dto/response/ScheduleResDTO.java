package com.tavemakers.surf.domain.schedule.presentation.dto.response;

import com.tavemakers.surf.domain.schedule.domain.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "일정 개별 조회")
public record ScheduleResDTO(
        @Schema(description = "일정 ID", example = "5")
        Long scheduleId,
        @Schema(description = "일정 카테고리", example = "정규행사")
        String category,
        @Schema(description = "일정 제목", example = "만남의 장")
        String title,
        @Schema(description = "일정 시작 시간", example = "2025-11-15T14:00:00")
        LocalDateTime startAt,
        @Schema(description = "일정 종료 시간", example = "2025-11-15T16:00:00")
        LocalDateTime endAt,
        @Schema(description = "일정 장소", example = "세종대학교 광개토대왕관")
        String location,
        @Schema(description = "공지사항 연동 여부", example = "true/false")
        boolean mappedByPost,
        @Schema(description = "연동되어있는 공지사항의 ID", example = "12")
        Long postId
) {
    public static ScheduleResDTO fromEntity(Schedule schedule) {
        boolean mappedByPost = schedule.getPost() != null;
        Long postId = mappedByPost ? schedule.getPost().getId() : null;

        return new ScheduleResDTO(
                schedule.getId(),
                schedule.getCategory(),
                schedule.getTitle(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getLocation(),
                mappedByPost,
                postId
        );
    }

    public static List<ScheduleResDTO> fromEntities(List<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleResDTO::fromEntity)
                .toList();
    }
}
