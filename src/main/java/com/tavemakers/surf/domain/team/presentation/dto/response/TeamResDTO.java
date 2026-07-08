package com.tavemakers.surf.domain.team.presentation.dto.response;

import com.tavemakers.surf.domain.team.domain.entity.Team;
import com.tavemakers.surf.domain.team.domain.entity.TeamType;

public record TeamResDTO(
        Long teamId,
        Integer generation,
        TeamType type,
        String name,
        String description,
        Long leaderId,
        String leaderName,
        int memberCount
) {
    public static TeamResDTO from(Team team) {
        return new TeamResDTO(
                team.getId(),
                team.getGeneration(),
                team.getType(),
                team.getName(),
                team.getDescription(),
                team.getLeader().getId(),
                team.getLeader().getName(),
                team.getMemberCount()
        );
    }
}