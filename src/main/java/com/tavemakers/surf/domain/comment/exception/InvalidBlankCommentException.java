package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.exception.ErrorMessage.INVALID_BLANK_COMMENT;

public class InvalidBlankCommentException extends BaseException {
    public InvalidBlankCommentException() {
        super(INVALID_BLANK_COMMENT.getStatus(), INVALID_BLANK_COMMENT.getMessage());
    }
}
