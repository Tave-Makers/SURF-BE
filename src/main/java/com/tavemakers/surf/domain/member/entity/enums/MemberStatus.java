package com.tavemakers.surf.domain.member.entity.enums;

import com.tavemakers.surf.domain.member.exception.MemberStatusConvertException;

import java.util.List;

public enum MemberStatus {
    REGISTERING, // 가입중
    WAITING,     // 대기중
    APPROVED,    // 승인
    REJECTED,    // 거절됨
    WITHDRAWN    // 탈퇴됨
    ;

    public static List<MemberStatus> toList(List<String> rawMemberStatuses) {
        return rawMemberStatuses.stream()
                .map(MemberStatus::valueOf)
                .toList();
    }

}
