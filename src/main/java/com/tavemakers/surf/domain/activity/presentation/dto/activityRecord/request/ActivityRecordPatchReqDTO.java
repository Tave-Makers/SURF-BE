package com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request;

import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityType;

import java.time.LocalDate;

public record ActivityRecordPatchReqDTO(
        ActivityType activityType,
        LocalDate activityDate
) {
}
