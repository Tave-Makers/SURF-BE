package com.tavemakers.surf.domain.home.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.home.exception.ErrorMessage.EMPTY_HOME_BANNERS;

public class EmptyHomeBannersException extends BaseException {
    public EmptyHomeBannersException() {
        super(EMPTY_HOME_BANNERS.getStatus(), EMPTY_HOME_BANNERS.getMessage());
    }
}
