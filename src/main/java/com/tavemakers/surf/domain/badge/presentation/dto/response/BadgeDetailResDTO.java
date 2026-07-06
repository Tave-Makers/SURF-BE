package com.tavemakers.surf.domain.badge.presentation.dto.response;

import com.tavemakers.surf.domain.badge.domain.entity.Badge;
import lombok.Builder;

@Builder
public record BadgeDetailResDTO(
        Long badgeId,
        String name,
        String imageUrl,
        String description,
        String requirement
) {
    public static BadgeDetailResDTO from(Badge badge) {
        return new BadgeDetailResDTO(
                badge.getId(),
                badge.getName(),
                badge.getImageUrl(),
                badge.getDescription(),
                badge.getRequirement()
        );
    }
}