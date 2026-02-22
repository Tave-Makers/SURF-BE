package com.tavemakers.surf.domain.score.dto.response;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ScoreDetailResDTO(
        Long id,
        Long memberId,
        String memberName,
        Long teamId,
        String teamName,
        BigDecimal rewardPrefixSum,
        BigDecimal penaltyPrefixSum,
        BigDecimal score
) {
    public static ScoreDetailResDTO from(PersonalActivityScore score) {
        ScoreDetailResDTOBuilder builder = basicBuilder(score);
        if (score.isTeam()) {
            return builder.teamId(score.getTeam().getId())
                    .teamName(score.getTeam().getName())
                    .build();
        }

        return builder.memberId(score.getMember().getId())
                .memberName(score.getMember().getName())
                .build();
    }

    public static ScoreDetailResDTOBuilder basicBuilder(PersonalActivityScore score) {
        return ScoreDetailResDTO.builder()
                .id(score.getId())
                .rewardPrefixSum(score.getRewardPrefixSum())
                .penaltyPrefixSum(score.getPenaltyPrefixSum())
                .score(score.getScore());
    }
}
