package com.tavemakers.surf.domain.board.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.exception.ErrorMessage.CATEGORY_NOT_FOUND;

public class CategoryNotFoundException extends BaseException {
    public CategoryNotFoundException() {
        super(CATEGORY_NOT_FOUND.getStatus(), CATEGORY_NOT_FOUND.getMessage());
    }
}
