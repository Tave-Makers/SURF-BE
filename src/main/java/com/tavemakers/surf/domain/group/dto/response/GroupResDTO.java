package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.GroupType;

public record GroupResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String groupName,
        String leaderName,
        int memberCount
) {}