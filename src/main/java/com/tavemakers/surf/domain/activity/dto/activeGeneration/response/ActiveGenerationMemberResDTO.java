package com.tavemakers.surf.domain.activity.dto.activeGeneration.response;

import com.tavemakers.surf.domain.member.entity.Member;

public record ActiveGenerationMemberResDTO(
        Long memberId,
        String name,
        String profileImageUrl
) {
    public static ActiveGenerationMemberResDTO from(Member member) {
        return new ActiveGenerationMemberResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl()
        );
    }
}
