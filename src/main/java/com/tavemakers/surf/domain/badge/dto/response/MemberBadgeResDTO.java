package com.tavemakers.surf.domain.badge.dto.response;

import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.member.dto.response.TrackResDTO;
import com.tavemakers.surf.domain.member.entity.Member;

import java.time.LocalDate;
import java.util.List;

public record MemberBadgeResDTO(
        Long memberId,
        String username,
        String profileImageUrl,
        List<TrackResDTO> trackList,
        LocalDate awardedAt
) {

    public static MemberBadgeResDTO from(MemberBadge memberBadge) {

        Member member = memberBadge.getMember();

        List<TrackResDTO> trackList = member.getTracks()
                .stream()
                .map(TrackResDTO::from)
                .toList();

        return new MemberBadgeResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                trackList,
                memberBadge.getAwardedAt()
        );
    }
}