package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.PASSWORD_ENCRYPTION_FAILED;

public class PasswordEncryptionException extends BaseException {
    public PasswordEncryptionException() {
        super(PASSWORD_ENCRYPTION_FAILED.getStatus(), PASSWORD_ENCRYPTION_FAILED.getMessage());

    }
}
