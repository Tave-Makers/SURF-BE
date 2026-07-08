package com.tavemakers.surf.domain.scrap.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.scrap.domain.exception.ErrorMessage.SCRAP_NOT_FOUND;

public class ScrapNotFoundException extends BaseException {
    public ScrapNotFoundException() {
        super(SCRAP_NOT_FOUND.getStatus(), SCRAP_NOT_FOUND.getMessage());
    }
}
