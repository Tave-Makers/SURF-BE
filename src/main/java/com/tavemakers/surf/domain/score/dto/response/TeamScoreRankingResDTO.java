package com.tavemakers.surf.domain.score.dto.response;

import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;

import java.math.BigDecimal;

public record TeamScoreRankingResDTO(
        Long teamId,
        String teamName,
        TeamType teamType,
        BigDecimal rewardTotal,
        BigDecimal penaltyTotal,
        BigDecimal totalScore
) {
    /** 팀별 상/벌점 현황 DTO 생성 */
    public static TeamScoreRankingResDTO of(Team team,
                                             BigDecimal rewardTotal, BigDecimal penaltyTotal,
                                             BigDecimal totalScore) {
        return new TeamScoreRankingResDTO(
                team.getId(),
                team.getName(),
                team.getType(),
                rewardTotal,
                penaltyTotal,
                totalScore
        );
    }
}
