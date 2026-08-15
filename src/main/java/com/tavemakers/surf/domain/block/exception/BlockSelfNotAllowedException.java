package com.tavemakers.surf.domain.block.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.block.exception.ErrorMessage.BLOCK_SELF_NOT_ALLOWED;

public class BlockSelfNotAllowedException extends BaseException {
    public BlockSelfNotAllowedException() {
        super(BLOCK_SELF_NOT_ALLOWED.getStatus(), BLOCK_SELF_NOT_ALLOWED.getMessage());
    }
}
