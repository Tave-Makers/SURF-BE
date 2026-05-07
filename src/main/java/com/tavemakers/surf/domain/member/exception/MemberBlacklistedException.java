package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_BLACKLISTED;

public class MemberBlacklistedException extends BaseException {
    public MemberBlacklistedException() {
        super(MEMBER_BLACKLISTED.getStatus(), MEMBER_BLACKLISTED.getMessage());
    }
}
