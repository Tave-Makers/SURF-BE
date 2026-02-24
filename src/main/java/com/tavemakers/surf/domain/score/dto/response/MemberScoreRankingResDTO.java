package com.tavemakers.surf.domain.score.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.Part;

import java.math.BigDecimal;

public record MemberScoreRankingResDTO(
        Long memberId,
        String profileImageUrl,
        String name,
        String part,
        BigDecimal rewardTotal,
        BigDecimal penaltyTotal,
        BigDecimal totalScore
) {
    /** 개인별 상/벌점 현황 DTO 생성 */
    public static MemberScoreRankingResDTO of(Member member, Part part,
                                               BigDecimal rewardTotal, BigDecimal penaltyTotal,
                                               BigDecimal totalScore) {
        return new MemberScoreRankingResDTO(
                member.getId(),
                member.getProfileImageUrl(),
                member.getName(),
                part != null ? part.getDisplayName() : null,
                rewardTotal,
                penaltyTotal,
                totalScore
        );
    }
}
