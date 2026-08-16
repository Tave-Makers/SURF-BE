package com.tavemakers.surf.domain.block.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.block.exception.ErrorMessage.BLOCK_NOT_FOUND;

public class BlockNotFoundException extends BaseException {
    public BlockNotFoundException() {
        super(BLOCK_NOT_FOUND.getStatus(), BLOCK_NOT_FOUND.getMessage());
    }
}
