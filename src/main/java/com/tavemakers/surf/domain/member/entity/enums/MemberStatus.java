package com.tavemakers.surf.domain.member.entity.enums;

import java.util.List;

public enum MemberStatus {
    REGISTERING, // 가입중
    WAITING,     // 대기중
    APPROVED,    // 승인
    REJECTED,    // 거절됨
    WITHDRAWN    // 탈퇴됨
    ;

    public static List<MemberStatus> valueOf(List<String> rawMemberStatuses) {
        return rawMemberStatuses.stream()
                .map(MemberStatus::valueOf)
                .toList();
    }

}
