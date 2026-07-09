package com.tavemakers.surf.presentation.team.dto.response;


import java.util.List;

public record TeamGenerationSectionResDTO(
        Integer generation,
        List<TeamListResDTO> teams
) {}