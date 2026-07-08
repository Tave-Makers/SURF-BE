package com.tavemakers.surf.domain.activity.presentation.dto.activityRecord.request;

import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ActivityType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ActivityRecordReqDTO(
        @NotNull List<Long> memberIdList,
        ActivityCategory category, // MVP 1에서는 미정.
        @NotNull ActivityType activityName,
        @NotNull LocalDate activityDate
) {
}