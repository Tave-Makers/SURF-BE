package com.tavemakers.surf.domain.feedback.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.feedback.domain.exception.ErrorMessage.TOO_MANY_FEEDBACK;

public class TooManyFeedbackException extends BaseException {
    public TooManyFeedbackException() {
        super(TOO_MANY_FEEDBACK.getStatus(), TOO_MANY_FEEDBACK.getMessage());
    }
}