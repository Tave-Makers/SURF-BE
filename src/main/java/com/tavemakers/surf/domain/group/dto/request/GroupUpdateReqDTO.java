package com.tavemakers.surf.domain.group.dto.request;

import java.util.List;

public record GroupUpdateReqDTO(
        String name,
        String description,
        Long leaderMemberId,
        List<Long> addMemberIds,
        List<Long> removeMemberIds
) {}