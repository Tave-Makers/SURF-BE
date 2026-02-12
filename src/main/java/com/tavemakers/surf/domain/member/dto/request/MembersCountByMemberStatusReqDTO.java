package com.tavemakers.surf.domain.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;

import java.util.List;

public record MembersCountByMemberStatusReqDTO(
        List<MemberStatus> memberStatuses
) {
}
