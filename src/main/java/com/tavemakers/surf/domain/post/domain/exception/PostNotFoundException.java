package com.tavemakers.surf.domain.post.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.domain.exception.ErrorMessage.POST_NOT_FOUND;

public class PostNotFoundException extends BaseException {
    public PostNotFoundException() {
        super(POST_NOT_FOUND.getStatus(), POST_NOT_FOUND.getMessage());
    }
}
