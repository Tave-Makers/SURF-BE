package com.tavemakers.surf.domain.team.presentation.dto.response;

import com.tavemakers.surf.domain.team.domain.entity.Team;
import com.tavemakers.surf.domain.team.domain.entity.TeamType;

public record TeamListResDTO(
        Long teamId,
        Integer generation,
        TeamType type,
        String name
) {
    public static TeamListResDTO from(Team team) {
        return new TeamListResDTO(
                team.getId(),
                team.getGeneration(),
                team.getType(),
                team.getName()
        );
    }
}