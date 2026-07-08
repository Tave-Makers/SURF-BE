package com.tavemakers.surf.domain.post.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.domain.exception.ErrorMessage.POST_IMAGE_DELETE_DENIED;

public class PostImageDeleteAccessDeniedException extends BaseException {
    public PostImageDeleteAccessDeniedException() {
        super(POST_IMAGE_DELETE_DENIED.getStatus(), POST_IMAGE_DELETE_DENIED.getMessage());
    }
}
