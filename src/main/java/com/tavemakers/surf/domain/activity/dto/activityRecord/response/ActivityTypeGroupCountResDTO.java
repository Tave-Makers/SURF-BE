package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ActivityTypeGroupCountResDTO (
        int totalCount,
        List<ActivityTypeCountResDTO> list
){
    public static ActivityTypeGroupCountResDTO of(List<ActivityTypeCountResDTO> group) {
        int totalCount = group.stream()
                .map(ActivityTypeCountResDTO::count)
                .mapToInt(Long::intValue)
                .sum();

        return ActivityTypeGroupCountResDTO.builder()
                .list(group)
                .totalCount(totalCount)
                .build();
    }
}
