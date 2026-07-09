package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.PASSWORD_NOT_SETTING;

public class PasswordNotSettingException extends BaseException {
    public PasswordNotSettingException() {
        super(PASSWORD_NOT_SETTING.getStatus(), PASSWORD_NOT_SETTING.getMessage());
    }
}
