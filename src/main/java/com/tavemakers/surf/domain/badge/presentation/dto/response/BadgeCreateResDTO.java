package com.tavemakers.surf.domain.badge.presentation.dto.response;

public record BadgeCreateResDTO(
        Long badgeId
) {
    public static BadgeCreateResDTO of(Long badgeId) {
        return new BadgeCreateResDTO(badgeId);
    }
}