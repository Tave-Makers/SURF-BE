package com.tavemakers.surf.domain.activity.dto.response;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.AppliedTarget;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import lombok.Builder;

@Builder
public record ActivityTypeDetailResDTO(
        String typeName,
        String displayName,
        Integer delta,
        ScoreType scoreType,
        AppliedTarget appliedTarget,
        String category
) {
    public static ActivityTypeDetailResDTO of(ActivityType activityType) {
        return ActivityTypeDetailResDTO.builder()
                .typeName(activityType.name())
                .displayName(activityType.getDisplayName())
                .delta(activityType.getDelta())
                .scoreType(activityType.getScoreType())
                .appliedTarget(activityType.getAppliedTarget())
                .category(activityType.getCategory().getDisplayName())
                .build();
    }
}
