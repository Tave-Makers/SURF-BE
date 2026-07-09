package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.exception.ErrorMessage.ALREADY_DELETED_COMMENT;

public class AlreadyDeletedCommentException extends BaseException {
    public AlreadyDeletedCommentException() {
        super(ALREADY_DELETED_COMMENT.getStatus(), ALREADY_DELETED_COMMENT.getMessage());
    }
}
