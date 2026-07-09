package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_DISMISS_NOT_ALLOWED;

public class MemberDismissNotAllowedException extends BaseException {
    public MemberDismissNotAllowedException() {
        super(MEMBER_DISMISS_NOT_ALLOWED.getStatus(), MEMBER_DISMISS_NOT_ALLOWED.getMessage());
    }
}
