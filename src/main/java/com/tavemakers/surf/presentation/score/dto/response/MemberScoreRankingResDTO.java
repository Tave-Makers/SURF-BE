package com.tavemakers.surf.presentation.score.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.Part;

import java.math.BigDecimal;

public record MemberScoreRankingResDTO(
        Long memberId,
        String profileImageUrl,
        String name,
        Integer generation,
        String part,
        BigDecimal rewardTotal,
        BigDecimal penaltyTotal,
        BigDecimal totalScore
) {
    public static MemberScoreRankingResDTO of(
            Member member,
            Integer generation,
            Part part,
            BigDecimal rewardTotal,
            BigDecimal penaltyTotal,
            BigDecimal totalScore
    ) {
        return new MemberScoreRankingResDTO(
                member.getId(),
                member.getProfileImageUrl(),
                member.getName(),
                generation,
                part != null ? part.getDisplayName() : null,
                rewardTotal,
                penaltyTotal,
                totalScore
        );
    }
}
