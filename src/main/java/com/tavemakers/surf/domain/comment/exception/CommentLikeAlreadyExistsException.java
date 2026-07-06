package com.tavemakers.surf.domain.comment.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.comment.exception.ErrorMessage.COMMENT_LIKE_ALREADY_EXISTS;

public class CommentLikeAlreadyExistsException extends BaseException {

    public CommentLikeAlreadyExistsException() {
        super(COMMENT_LIKE_ALREADY_EXISTS.getStatus(),
                COMMENT_LIKE_ALREADY_EXISTS.getMessage());
    }
}
