package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INCLUDED_WITHDRAWN_MEMBER;

public class IncludedWithdrawnMemberException extends BaseException {
    public IncludedWithdrawnMemberException() {
        super(INCLUDED_WITHDRAWN_MEMBER.getStatus(), INCLUDED_WITHDRAWN_MEMBER.getMessage());
    }
}
