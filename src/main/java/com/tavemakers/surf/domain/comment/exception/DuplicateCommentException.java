package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.exception.ErrorMessage.DUPLICATE_COMMENT;

public class DuplicateCommentException extends BaseException {

    public DuplicateCommentException() {
        super(DUPLICATE_COMMENT.getStatus(), DUPLICATE_COMMENT.getMessage());
    }
}
