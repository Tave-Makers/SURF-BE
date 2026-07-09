package com.tavemakers.surf.domain.board.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.exception.ErrorMessage.BOARD_CATEGORY_ALREADY_EXISTS;

public class BoardCategoryAlreadyExistsException extends BaseException {
    public BoardCategoryAlreadyExistsException() {
        super(BOARD_CATEGORY_ALREADY_EXISTS.getStatus(), BOARD_CATEGORY_ALREADY_EXISTS.getMessage());
    }
}
