package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.MemberBadge;

import java.time.LocalDate;

public record MemberOwnedBadgeResDTO(
        Long badgeId,
        String badgeName,
        String badgeImageUrl,
        String description,
        LocalDate awardedAt
) {
    public static MemberOwnedBadgeResDTO from(MemberBadge memberBadge) {

        return new MemberOwnedBadgeResDTO(
                memberBadge.getBadge().getId(),
                memberBadge.getBadge().getName(),
                memberBadge.getBadge().getImageUrl(),
                memberBadge.getBadge().getDescription(),
                memberBadge.getAwardedAt()
        );
    }
}