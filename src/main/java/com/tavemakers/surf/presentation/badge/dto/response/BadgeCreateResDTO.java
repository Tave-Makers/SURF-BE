package com.tavemakers.surf.presentation.badge.dto.response;

public record BadgeCreateResDTO(
        Long badgeId
) {
    public static BadgeCreateResDTO of(Long badgeId) {
        return new BadgeCreateResDTO(badgeId);
    }
}