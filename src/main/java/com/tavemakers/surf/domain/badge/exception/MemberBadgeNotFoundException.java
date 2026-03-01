package com.tavemakers.surf.domain.badge.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.badge.exception.ErrorMessage.MEMBER_BADGE_NOT_FOUND;

public class MemberBadgeNotFoundException extends BaseException {

    public MemberBadgeNotFoundException() {
        super(MEMBER_BADGE_NOT_FOUND.getStatus(),
                MEMBER_BADGE_NOT_FOUND.getMessage());
    }
}