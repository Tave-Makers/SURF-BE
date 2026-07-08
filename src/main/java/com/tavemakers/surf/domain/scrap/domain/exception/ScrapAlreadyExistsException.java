package com.tavemakers.surf.domain.scrap.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.scrap.domain.exception.ErrorMessage.SCRAP_ALREADY_EXISTS;

public class ScrapAlreadyExistsException extends BaseException {
    public ScrapAlreadyExistsException() {
        super(SCRAP_ALREADY_EXISTS.getStatus(), SCRAP_ALREADY_EXISTS.getMessage());
    }
}
