package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

public class NotMyCommentException extends BaseException {
    public NotMyCommentException() {
        super(ErrorMessage.NOT_MY_COMMENT.getStatus(), ErrorMessage.NOT_MY_COMMENT.getMessage());
    }
}
