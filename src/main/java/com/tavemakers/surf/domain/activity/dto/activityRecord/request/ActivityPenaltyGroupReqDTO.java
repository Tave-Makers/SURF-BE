package com.tavemakers.surf.domain.activity.dto.activityRecord.request;

import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityTypeGroupCountResDTO;
import lombok.Builder;

@Builder
public record ActivityPenaltyGroupReqDTO(
        ActivityTypeGroupCountResDTO late,
        ActivityTypeGroupCountResDTO absence
) {
    public static ActivityPenaltyGroupReqDTO of(ActivityTypeGroupCountResDTO late, ActivityTypeGroupCountResDTO absence) {
        return ActivityPenaltyGroupReqDTO.builder()
                .late(late)
                .absence(absence)
                .build();
    }
}
