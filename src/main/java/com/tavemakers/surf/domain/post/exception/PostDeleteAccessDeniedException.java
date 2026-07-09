package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.POST_DELETED_DENIED;

public class PostDeleteAccessDeniedException extends BaseException {
    public PostDeleteAccessDeniedException() {
        super(POST_DELETED_DENIED.getStatus(), POST_DELETED_DENIED.getMessage());
    }
}
