package com.tavemakers.surf.domain.activity.dto.activityRecord.response;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import lombok.Builder;

@Builder
public record ActivityCategoryResDTO(
        String categoryName,
        String categoryDisplayName
) {
    public static ActivityCategoryResDTO of(ActivityCategory category) {
        return ActivityCategoryResDTO.builder()
                .categoryName(category.name())
                .categoryDisplayName(category.getDisplayName())
                .build();
    }
}
