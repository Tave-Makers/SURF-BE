package com.tavemakers.surf.domain.member.dto.response;

import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record MembersCountByMemberStatusResDTO(
        List<MemberStatus> appliedMemberStatus,
        long membersCount
) {
    public static MembersCountByMemberStatusResDTO of(List<MemberStatus> memberStatuses , long membersCount) {
        return MembersCountByMemberStatusResDTO.builder()
                .appliedMemberStatus(memberStatuses)
                .membersCount(membersCount)
                .build();
    }
}
