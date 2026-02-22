package com.tavemakers.surf.domain.activity.dto.response;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import lombok.Builder;

import java.util.List;

@Builder
public record ActivityCategoryDetailResDTO(
        ActivityCategoryResDTO category,
        List<ActivityTypeDetailResDTO> activityTypeList
) {
    public static ActivityCategoryDetailResDTO of(ActivityCategory category, List<ActivityTypeDetailResDTO> activityTypeList) {
        return ActivityCategoryDetailResDTO.builder()
                .category(ActivityCategoryResDTO.of(category))
                .activityTypeList(activityTypeList)
                .build();
    }
}
