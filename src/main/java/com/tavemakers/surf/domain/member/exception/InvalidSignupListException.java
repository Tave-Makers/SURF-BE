package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INVALID_SIGNUP_LIST;

public class InvalidSignupListException extends BaseException {
    public InvalidSignupListException() {
        super(INVALID_SIGNUP_LIST.getStatus(), INVALID_SIGNUP_LIST.getMessage());
    }
}