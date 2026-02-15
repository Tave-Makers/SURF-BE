package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.member.entity.Track;

import java.util.List;

public record GroupMemberResDTO(
        Long memberId,
        List<Track> track
) {}
