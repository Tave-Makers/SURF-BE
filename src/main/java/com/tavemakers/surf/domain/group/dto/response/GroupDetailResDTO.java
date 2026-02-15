package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.GroupType;

import java.util.List;

public record GroupDetailResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String groupName,
        String leaderName,
        String description,
        List<GroupMemberResDTO> members
) {}