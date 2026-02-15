package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;

public record GroupResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String name,
        String description,
        Long leaderId,
        String leaderName,
        int memberCount
) {
    public static GroupResDTO from(Group group) {
        return new GroupResDTO(
                group.getId(),
                group.getGeneration(),
                group.getType(),
                group.getName(),
                group.getDescription(),
                group.getLeader().getId(),
                group.getLeader().getName(),
                group.getMemberCount()
        );
    }
}