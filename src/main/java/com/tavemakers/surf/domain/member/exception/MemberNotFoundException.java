package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_NOT_FOUND;

public class MemberNotFoundException extends BaseException {
    public MemberNotFoundException() {

        super(MEMBER_NOT_FOUND.getStatus(), MEMBER_NOT_FOUND.getMessage());
    }

    public MemberNotFoundException(Long memberId) {
        super(MEMBER_NOT_FOUND.getStatus(),
                MEMBER_NOT_FOUND.getMessage() + " (id=" + memberId + ")");
    }
}
