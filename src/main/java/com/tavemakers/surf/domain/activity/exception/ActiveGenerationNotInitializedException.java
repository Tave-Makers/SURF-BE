package com.tavemakers.surf.domain.activity.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.activity.exception.ErrorMessage.ACTIVE_GENERATION_NOT_INITIALIZED;

public class ActiveGenerationNotInitializedException extends BaseException {
    public ActiveGenerationNotInitializedException() {
        super(ACTIVE_GENERATION_NOT_INITIALIZED.getStatus(), ACTIVE_GENERATION_NOT_INITIALIZED.getMessage());
    }
}
