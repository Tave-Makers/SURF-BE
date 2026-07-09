package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.POST_IMAGE_NOT_FOUND;

public class PostImageNotFoundException extends BaseException {
    public PostImageNotFoundException() {
        super(POST_IMAGE_NOT_FOUND.getStatus(), POST_IMAGE_NOT_FOUND.getMessage());
    }
}
