package com.tavemakers.surf.domain.post.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.domain.exception.ErrorMessage.POST_LIKE_ALREADY_EXISTS;

public class PostLikeAlreadyExistsException extends BaseException {

    public PostLikeAlreadyExistsException() {
        super(POST_LIKE_ALREADY_EXISTS.getStatus(),
                POST_LIKE_ALREADY_EXISTS.getMessage());
    }
}
