package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Builder
public record ActivityRecordResDTO(
        Long memberId,
        String categoryName,
        String activityName,
        String scoreType,
        String activityDate,
        BigDecimal prefixSum,
        BigDecimal appliedScore
) {

    public static ActivityRecordResDTO from(ActivityRecord activityRecord) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd");
        String formattedDate = activityRecord.getActivityDate().format(formatter);

        String category = null;
        String activityName = null;

        // 대주제가 Null인 경우,
        if (activityRecord.getCategory() == null) {
            category = activityRecord.getActivityType().getDisplayName();
        } else {
            category = activityRecord.getCategory().name(); // TODO 추후 displayName으로 수정
            activityName = activityRecord.getActivityType().getDisplayName();
        }

        return ActivityRecordResDTO.builder()
                .memberId(activityRecord.getMemberId())
                .categoryName(category)
                .activityName(activityName)
                .activityDate(formattedDate)
                .scoreType(activityRecord.getScoreType().name())
                .prefixSum(activityRecord.getPrefixSum())
                .appliedScore(activityRecord.getAppliedScore())
                .build();
    }

}
