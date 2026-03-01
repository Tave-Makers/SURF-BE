package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;

import java.time.LocalDate;
import java.util.Comparator;

public record MemberBadgeResDTO(
        Long memberId,
        String username,
        String profileImageUrl,
        Integer generation,
        LocalDate awardedAt
) {

    public static MemberBadgeResDTO from(MemberBadge memberBadge) {

        Member member = memberBadge.getMember();

        Integer firstGeneration = member.getTracks()
                .stream()
                .min(Comparator.comparing(Track::getGeneration))
                .map(Track::getGeneration)
                .orElse(null);

        return new MemberBadgeResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                firstGeneration,
                memberBadge.getAwardedAt()
        );
    }
}