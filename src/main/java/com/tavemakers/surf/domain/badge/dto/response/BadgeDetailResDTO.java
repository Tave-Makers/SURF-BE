package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.Badge;
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
        return BadgeDetailResDTO.builder()
                        .badgeId(badge.getId())
                        .name(badge.getName())
                        .imageUrl(badge.getImageUrl())
                        .description(badge.getDescription())
                        .requirement(badge.getRequirement())
                        .build();
    }
}