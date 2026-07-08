package com.tavemakers.surf.domain.comment.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.domain.exception.ErrorMessage.COMMENT_NOT_FOUND;

public class CommentNotFoundException extends BaseException {
    public CommentNotFoundException() {
        super(COMMENT_NOT_FOUND.getStatus(), COMMENT_NOT_FOUND.getMessage());
    }
}
