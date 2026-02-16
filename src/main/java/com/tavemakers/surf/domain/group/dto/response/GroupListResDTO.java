package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;

public record GroupListResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String name
) {
    public static GroupListResDTO from(Group g) {
        return new GroupListResDTO(
                g.getId(),
                g.getGeneration(),
                g.getType(),
                g.getName()
        );
    }
}