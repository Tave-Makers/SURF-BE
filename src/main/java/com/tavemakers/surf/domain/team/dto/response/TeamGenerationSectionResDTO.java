package com.tavemakers.surf.domain.team.dto.response;


import java.util.List;

public record TeamGenerationSectionResDTO(
        Integer generation,
        List<TeamListResDTO> teams
) {}