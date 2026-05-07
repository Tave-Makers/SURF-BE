package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_SIGNUP_REJECTED;

public class MemberSignupRejectedException extends BaseException {
    public MemberSignupRejectedException() {
        super(MEMBER_SIGNUP_REJECTED.getStatus(), MEMBER_SIGNUP_REJECTED.getMessage());
    }
}
