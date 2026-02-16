package com.tavemakers.surf.domain.group.dto.response;


import java.util.List;

public record GroupGenerationSectionResDTO(
        Integer generation,
        List<GroupListResDTO> groups
) {}