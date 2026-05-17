package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.POST_FILE_NOT_FOUND;

public class PostFileNotFoundException extends BaseException {
    public PostFileNotFoundException() {
        super(POST_FILE_NOT_FOUND.getStatus(), POST_FILE_NOT_FOUND.getMessage());
    }
}
