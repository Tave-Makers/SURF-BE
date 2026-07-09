package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MEMBER_STATUS_CANNOT_CONVERT;

public class MemberStatusConvertException extends BaseException {
    public MemberStatusConvertException() {
        super(MEMBER_STATUS_CANNOT_CONVERT.getStatus(), MEMBER_STATUS_CANNOT_CONVERT.getMessage());
    }
}
