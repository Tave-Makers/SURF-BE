package com.tavemakers.surf.presentation.schedule.dto.response;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
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
        @Schema(description = "조회 가능한 연결 공지 존재 여부", example = "true")
        boolean mappedByPost,
        @Schema(description = "연결된 공지 목록 (예약 공지 제외, 최신순)", example = "[{\"postId\":12,\"title\":\"만남의 장 추가 안내\"},{\"postId\":11,\"title\":\"만남의 장 공지\"}]")
        List<SchedulePostResDTO> posts
) {
    public static ScheduleResDTO fromEntity(Schedule schedule, List<SchedulePostResDTO> posts) {
        return new ScheduleResDTO(
                schedule.getId(), schedule.getCategory(), schedule.getTitle(),
                schedule.getStartAt(), schedule.getEndAt(), schedule.getLocation(),
                !posts.isEmpty(), List.copyOf(posts));
    }
}
