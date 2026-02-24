package com.tavemakers.surf.domain.badge.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.badge.exception.ErrorMessage.BADGE_NOT_FOUND;

public class BadgeNotFoundException extends BaseException {

    public BadgeNotFoundException() {
        super(BADGE_NOT_FOUND.getStatus(), BADGE_NOT_FOUND.getMessage());
    }
}