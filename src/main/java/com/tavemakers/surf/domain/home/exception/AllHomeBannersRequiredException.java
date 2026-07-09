package com.tavemakers.surf.domain.home.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.home.exception.ErrorMessage.ALL_HOME_BANNERS_REQUIRED;

public class AllHomeBannersRequiredException extends BaseException {
    public AllHomeBannersRequiredException() {
        super(ALL_HOME_BANNERS_REQUIRED.getStatus(), ALL_HOME_BANNERS_REQUIRED.getMessage());
    }
}
