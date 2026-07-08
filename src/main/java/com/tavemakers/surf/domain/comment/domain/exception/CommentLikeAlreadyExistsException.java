package com.tavemakers.surf.domain.comment.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.domain.exception.ErrorMessage.COMMENT_LIKE_ALREADY_EXISTS;

public class CommentLikeAlreadyExistsException extends BaseException {

    public CommentLikeAlreadyExistsException() {
        super(COMMENT_LIKE_ALREADY_EXISTS.getStatus(),
                COMMENT_LIKE_ALREADY_EXISTS.getMessage());
    }
}
