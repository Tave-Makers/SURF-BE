package com.tavemakers.surf.domain.group.dto.response;

import com.tavemakers.surf.domain.group.entity.Group;
import com.tavemakers.surf.domain.group.entity.GroupType;
import com.tavemakers.surf.domain.member.entity.Member;

import java.util.List;

public record GroupDetailResDTO(
        Long groupId,
        Integer generation,
        GroupType type,
        String name,
        String description,
        Long leaderId,
        String leaderName,
        List<GroupMemberResDTO> members
) {
    public static GroupDetailResDTO from(Group g, List<GroupMemberResDTO> members) {
        return new GroupDetailResDTO(
                g.getId(),
                g.getGeneration(),
                g.getType(),
                g.getName(),
                g.getDescription(),
                g.getLeader().getId(),
                g.getLeader().getName(),
                members
        );
    }

    public record GroupMemberResDTO(
            Long memberId,
            String name,
            String part,
            boolean isPartLeader
    ) {
        public static GroupMemberResDTO from(Member m) {
            return new GroupMemberResDTO(
                    m.getId(),
                    m.getName(),        // ✅ 프로젝트 Member 필드에 맞춰 수정
                    m.getPart(),        // ✅ 프로젝트 Member 필드에 맞춰 수정
                    m.isPartLeader()    // ✅ 프로젝트 Member 필드에 맞춰 수정
            );
        }
    }
}