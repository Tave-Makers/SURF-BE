package com.tavemakers.surf.domain.activity.dto.response;

import com.tavemakers.surf.domain.score.dto.response.MemberScoreRankingResDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record TeamMemberScoreListResDTO(
        Long teamId,
        String teamName,
        List<MemberScoreRankingResDTO> members
) {
    /** 팀 멤버별 점수 현황 DTO 생성 */
    public static TeamMemberScoreListResDTO of(Long teamId, String teamName, List<MemberScoreRankingResDTO> members) {
        return TeamMemberScoreListResDTO.builder()
                .teamId(teamId)
                .teamName(teamName)
                .members(members)
                .build();
    }
}
