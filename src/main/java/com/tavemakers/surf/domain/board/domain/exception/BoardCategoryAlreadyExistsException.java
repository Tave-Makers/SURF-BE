package com.tavemakers.surf.domain.board.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.domain.exception.ErrorMessage.BOARD_CATEGORY_ALREADY_EXISTS;

public class BoardCategoryAlreadyExistsException extends BaseException {
    public BoardCategoryAlreadyExistsException() {
        super(BOARD_CATEGORY_ALREADY_EXISTS.getStatus(), BOARD_CATEGORY_ALREADY_EXISTS.getMessage());
    }
}
