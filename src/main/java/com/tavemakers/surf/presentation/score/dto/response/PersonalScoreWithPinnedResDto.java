package com.tavemakers.surf.presentation.score.dto.response;

import com.tavemakers.surf.presentation.activity.dto.activityRecord.response.ActivityRecordSummaryResDTO;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PersonalScoreWithPinnedResDto(
        BigDecimal score,
        ActivityRecordSummaryResDTO records
) {

    public static PersonalScoreWithPinnedResDto of(BigDecimal score, ActivityRecordSummaryResDTO dtoList) {

        return PersonalScoreWithPinnedResDto.builder()
                .score(score)
                .records(dtoList)
                .build();
    }

}
