package com.tavemakers.surf.domain.post.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.domain.exception.ErrorMessage.POST_IMAGE_EMPTY;

public class PostImageListEmptyException extends BaseException {
    public PostImageListEmptyException() {
        super(POST_IMAGE_EMPTY.getStatus(), POST_IMAGE_EMPTY.getMessage());
    }
}
