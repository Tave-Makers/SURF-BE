package com.tavemakers.surf.domain.board.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.domain.exception.ErrorMessage.CATEGORY_REQUIRED;

public class CategoryRequiredException extends BaseException {
    public CategoryRequiredException() {
        super(CATEGORY_REQUIRED.getStatus(), CATEGORY_REQUIRED.getMessage());
    }
}
