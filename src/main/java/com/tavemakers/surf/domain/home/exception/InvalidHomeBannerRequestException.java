package com.tavemakers.surf.domain.home.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.home.exception.ErrorMessage.INVALID_HOME_BANNER_REQUEST;

public class InvalidHomeBannerRequestException extends BaseException {
    public InvalidHomeBannerRequestException() {
        super(INVALID_HOME_BANNER_REQUEST.getStatus(), INVALID_HOME_BANNER_REQUEST.getMessage());
    }
}
