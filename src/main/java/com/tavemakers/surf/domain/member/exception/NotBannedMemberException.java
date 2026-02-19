package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.NOT_BANNED_MEMBER;

public class NotBannedMemberException extends BaseException {

    public NotBannedMemberException() {
        super(NOT_BANNED_MEMBER.getStatus(), NOT_BANNED_MEMBER.getMessage());
    }
}
