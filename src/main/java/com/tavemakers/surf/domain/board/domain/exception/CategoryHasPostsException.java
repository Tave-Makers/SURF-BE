package com.tavemakers.surf.domain.board.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.domain.exception.ErrorMessage.CATEGORY_HAS_POSTS;

public class CategoryHasPostsException extends BaseException {
    public CategoryHasPostsException() {
        super(CATEGORY_HAS_POSTS.getStatus(), CATEGORY_HAS_POSTS.getMessage());
    }
}
