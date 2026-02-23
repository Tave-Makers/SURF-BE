package com.tavemakers.surf.domain.activity.dto.response;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record AdminActivityRecordResDTO(
        Long activityRecordId,
        ActivityType activityType,
        String activityName,
        ScoreType scoreType,
        LocalDate activityDate,
        BigDecimal appliedScore
) {
    /** 관리자용 활동기록 응답 DTO 생성 */
    public static AdminActivityRecordResDTO from(ActivityRecord record) {
        return AdminActivityRecordResDTO.builder()
                .activityRecordId(record.getId())
                .activityType(record.getActivityType())
                .activityName(record.getActivityType().getDisplayName())
                .scoreType(record.getScoreType())
                .activityDate(record.getActivityDate())
                .appliedScore(record.getAppliedScore())
                .build();
    }
}
