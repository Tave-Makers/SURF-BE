package com.tavemakers.surf.domain.post.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.domain.exception.ErrorMessage.POST_FILE_DELETE_DENIED;

public class PostFileDeleteAccessDeniedException extends BaseException {
    public PostFileDeleteAccessDeniedException() {
        super(POST_FILE_DELETE_DENIED.getStatus(), POST_FILE_DELETE_DENIED.getMessage());
    }
}
