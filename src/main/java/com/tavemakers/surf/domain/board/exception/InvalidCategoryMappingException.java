package com.tavemakers.surf.domain.board.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.exception.ErrorMessage.INVALID_CATEGORY_MAPPING;

public class InvalidCategoryMappingException extends BaseException {
    public InvalidCategoryMappingException() {
        super(INVALID_CATEGORY_MAPPING.getStatus(), INVALID_CATEGORY_MAPPING.getMessage());
    }
}
