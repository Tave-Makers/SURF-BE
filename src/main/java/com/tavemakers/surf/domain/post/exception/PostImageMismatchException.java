package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.POST_IMAGE_MISMATCH;

public class PostImageMismatchException extends BaseException {
    public PostImageMismatchException() {
        super(POST_IMAGE_MISMATCH.getStatus(), POST_IMAGE_MISMATCH.getMessage());
    }
}
