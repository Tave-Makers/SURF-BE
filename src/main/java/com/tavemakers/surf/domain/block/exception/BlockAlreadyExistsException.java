package com.tavemakers.surf.domain.block.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.block.exception.ErrorMessage.BLOCK_ALREADY_EXISTS;

public class BlockAlreadyExistsException extends BaseException {
    public BlockAlreadyExistsException() {
        super(BLOCK_ALREADY_EXISTS.getStatus(), BLOCK_ALREADY_EXISTS.getMessage());
    }
}
