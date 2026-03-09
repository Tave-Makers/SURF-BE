package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import lombok.Builder;

@Builder
public record ActivityTypeCountResDTO(
        String activityType,
        Long count
) {
    public static ActivityTypeCountResDTO of(ActivityType type, Long count) {
        return ActivityTypeCountResDTO.builder()
                .activityType(type.name())
                .count(count)
                .build();
    }

    public static ActivityTypeCountResDTO of(ActivityType type, int count) {
        return ActivityTypeCountResDTO.builder()
                .activityType(type.name())
                .count(Long.valueOf(count))
                .build();
    }
}