package com.tavemakers.surf.domain.moderation.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.moderation.exception.ErrorMessage.MODERATION_TERM_NOT_FOUND;

public class ModerationTermNotFoundException extends BaseException {
    public ModerationTermNotFoundException() {
        super(MODERATION_TERM_NOT_FOUND.getStatus(), MODERATION_TERM_NOT_FOUND.getMessage());
    }
}
