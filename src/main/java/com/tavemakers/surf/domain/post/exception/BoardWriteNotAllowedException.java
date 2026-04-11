package com.tavemakers.surf.domain.post.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.post.exception.ErrorMessage.BOARD_WRITE_NOT_ALLOWED;

public class BoardWriteNotAllowedException extends BaseException {
    public BoardWriteNotAllowedException() {
        super(BOARD_WRITE_NOT_ALLOWED.getStatus(), BOARD_WRITE_NOT_ALLOWED.getMessage());
    }
}
