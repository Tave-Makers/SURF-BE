package com.tavemakers.surf.domain.group.dto.request;

import com.tavemakers.surf.domain.group.entity.GroupType;

import java.util.List;

public record GroupCreateReqDTO(
        Integer generation,
        GroupType type,
        String name,
        String description,
        Long leaderMemberId,
        List<Long> memberIds
) {}
