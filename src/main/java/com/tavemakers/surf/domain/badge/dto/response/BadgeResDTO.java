package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.Badge;

public record BadgeResDTO(
        Long badgeId,
        String name,
        String imageUrl,
        String description,
        String requirement
) {

    public static BadgeResDTO from(Badge badge) {
        return new BadgeResDTO(
                badge.getId(),
                badge.getName(),
                badge.getImageUrl(),
                badge.getDescription(),
                badge.getRequirement()
        );
    }
}