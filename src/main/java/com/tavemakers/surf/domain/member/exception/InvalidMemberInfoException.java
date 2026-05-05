package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INVALID_MEMBER_INFO;

public class InvalidMemberInfoException extends BaseException {
    public InvalidMemberInfoException() {
        super(INVALID_MEMBER_INFO.getStatus(), INVALID_MEMBER_INFO.getMessage());
    }

    public InvalidMemberInfoException(String message) {
        super(INVALID_MEMBER_INFO.getStatus(), message);
    }
}
