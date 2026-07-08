package com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.response;

import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityPenaltyGroupReqDTO;
import com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request.ActivityRewardGroupReqDTO;
import lombok.Builder;

@Builder
public record ActivityRecordSummaryResDTO(
        ActivityRewardGroupReqDTO rewards,
        ActivityPenaltyGroupReqDTO penalties
) {
    public static ActivityRecordSummaryResDTO of(
            ActivityRewardGroupReqDTO rewards,
            ActivityPenaltyGroupReqDTO penalties
    ) {
        return ActivityRecordSummaryResDTO.builder()
                .rewards(rewards)
                .penalties(penalties)
                .build();
    }

}
