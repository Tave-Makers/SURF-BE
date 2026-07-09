package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_ALREADY_EXISTS;

public class MemberAlreadyExistsException extends BaseException {
    public MemberAlreadyExistsException() {
        super(MEMBER_ALREADY_EXISTS.getStatus(), MEMBER_ALREADY_EXISTS.getMessage());
    }
}
