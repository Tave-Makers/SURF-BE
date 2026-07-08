package com.tavemakers.surf.domain.comment.domain.exception;

import com.tavemakers.surf.domain.comment.domain.exception.ErrorMessage;
import com.tavemakers.surf.global.common.exception.BaseException;

public class CannotLikeDeletedCommentException extends BaseException {

    public CannotLikeDeletedCommentException() {
        super(
                ErrorMessage.CANNOT_LIKE_DELETED_COMMENT.getStatus(),
                ErrorMessage.CANNOT_LIKE_DELETED_COMMENT.getMessage()
        );
    }
}
