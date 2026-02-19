package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ALREADY_BANNED_MEMBER;

public class AlreadyBannedMemberException extends BaseException {

    public AlreadyBannedMemberException() {
        super(ALREADY_BANNED_MEMBER.getStatus(), ALREADY_BANNED_MEMBER.getMessage());
    }
}
