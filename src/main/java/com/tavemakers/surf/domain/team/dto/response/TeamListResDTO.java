package com.tavemakers.surf.domain.team.dto.response;

import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;

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