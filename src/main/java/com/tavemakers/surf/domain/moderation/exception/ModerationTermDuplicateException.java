package com.tavemakers.surf.domain.moderation.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.moderation.exception.ErrorMessage.MODERATION_TERM_DUPLICATE;

public class ModerationTermDuplicateException extends BaseException {
    public ModerationTermDuplicateException() {
        super(MODERATION_TERM_DUPLICATE.getStatus(), MODERATION_TERM_DUPLICATE.getMessage());
    }
}
