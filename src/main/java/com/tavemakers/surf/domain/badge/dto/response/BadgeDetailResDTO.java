package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.Badge;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeDetailResDTO {

    private Long badgeId;
    private String name;
    private String imageUrl;
    private String description;
    private String requirement;

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