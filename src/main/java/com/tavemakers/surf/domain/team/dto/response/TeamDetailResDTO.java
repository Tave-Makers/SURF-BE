package com.tavemakers.surf.domain.team.dto.response;

import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.member.dto.response.TrackResDTO;

import java.util.List;

public record TeamDetailResDTO(
        Long teamId,
        Integer generation,
        TeamType type,
        String name,
        String description,
        MemberCardDTO leader,
        List<MemberCardDTO> members
) {
    public static TeamDetailResDTO from(Team team, MemberCardDTO leader, List<MemberCardDTO> members) {
        return new TeamDetailResDTO(
                team.getId(),
                team.getGeneration(),
                team.getType(),
                team.getName(),
                team.getDescription(),
                leader,
                members
        );
    }

    public record MemberCardDTO(
            Long memberId,
            String name,
            String profileImageUrl,
            List<TrackResDTO> tracks
    ) {}
}