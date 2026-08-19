package com.tavemakers.surf.domain.activity.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import java.util.List;
import java.util.stream.Collectors;

import static com.tavemakers.surf.domain.activity.exception.ErrorMessage.INACTIVE_MEMBER_INCLUDED;

public class InactiveMemberIncludedException extends BaseException {

    public InactiveMemberIncludedException(List<Long> inactiveMemberIds) {
        super(
                INACTIVE_MEMBER_INCLUDED.getStatus(),
                INACTIVE_MEMBER_INCLUDED.getMessage() + " memberIds=" + joinIds(inactiveMemberIds)
        );
    }

    private static String joinIds(List<Long> memberIds) {
        return memberIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
