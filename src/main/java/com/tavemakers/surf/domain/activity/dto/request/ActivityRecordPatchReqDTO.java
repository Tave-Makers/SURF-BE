package com.tavemakers.surf.domain.activity.dto.request;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;

import java.time.LocalDate;

public record ActivityRecordPatchReqDTO(
        ActivityType activityType,
        LocalDate activityDate
) {
}
