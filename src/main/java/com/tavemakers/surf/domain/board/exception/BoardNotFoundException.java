package com.tavemakers.surf.domain.board.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.board.exception.ErrorMessage.BOARD_NOT_FOUND;

public class BoardNotFoundException extends BaseException {
    public BoardNotFoundException() {
        super(BOARD_NOT_FOUND.getStatus(), BOARD_NOT_FOUND.getMessage());
    }
}
