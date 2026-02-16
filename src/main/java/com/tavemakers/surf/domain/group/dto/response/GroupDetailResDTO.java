package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;
import com.tavemakers.surf.domain.member.dto.response.TrackResDTO;

import java.util.List;

public record GroupDetailResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String name,
        String description,
        MemberCardDTO leader,
        List<MemberCardDTO> members
) {
    public static GroupDetailResDTO from(Group g, MemberCardDTO leader, List<MemberCardDTO> members) {
        return new GroupDetailResDTO(
                g.getId(),
                g.getGeneration(),
                g.getType(),
                g.getName(),
                g.getDescription(),
                leader,
                members
        );
    }

    public record MemberCardDTO(
            Long memberId,
            String name,
            String profileImageUrl,
            List<TrackResDTO> tracks
    ) {}
}