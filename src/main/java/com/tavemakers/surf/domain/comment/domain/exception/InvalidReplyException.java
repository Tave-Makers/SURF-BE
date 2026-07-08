package com.tavemakers.surf.domain.comment.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.domain.exception.ErrorMessage.INVALID_REPLY;

public class InvalidReplyException extends BaseException {

    public InvalidReplyException() {
        super(INVALID_REPLY.getStatus(), INVALID_REPLY.getMessage());
    }
}
