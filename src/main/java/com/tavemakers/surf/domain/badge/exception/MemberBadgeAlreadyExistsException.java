package com.tavemakers.surf.domain.badge.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.badge.exception.ErrorMessage.MEMBER_BADGE_ALREADY_EXISTS;

public class MemberBadgeAlreadyExistsException extends BaseException {

    public MemberBadgeAlreadyExistsException() {
        super(MEMBER_BADGE_ALREADY_EXISTS.getStatus(),
                MEMBER_BADGE_ALREADY_EXISTS.getMessage());
    }
}