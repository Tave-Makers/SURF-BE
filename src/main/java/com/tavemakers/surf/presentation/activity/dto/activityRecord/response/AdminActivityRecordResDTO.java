package com.tavemakers.surf.presentation.activity.dto.activityRecord.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record AdminActivityRecordResDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long activityRecordId,
        ActivityType activityType,
        String activityName,
        ScoreType scoreType,
        LocalDate activityDate,
        BigDecimal prefixSum,
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
                .prefixSum(record.getPrefixSum())
                .appliedScore(record.getAppliedScore())
                .build();
    }
}
