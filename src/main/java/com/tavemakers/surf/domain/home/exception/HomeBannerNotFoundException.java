package com.tavemakers.surf.domain.home.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.home.exception.ErrorMessage.HOME_BANNER_NOT_FOUND;

public class HomeBannerNotFoundException extends BaseException {
    public HomeBannerNotFoundException() {
        super(HOME_BANNER_NOT_FOUND.getStatus(), HOME_BANNER_NOT_FOUND.getMessage());
    }
}
