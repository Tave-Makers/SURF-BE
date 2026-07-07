package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.EMAIL_ALREADY_USED;

/** 온보딩 통합 이메일이 이미 다른 회원에게 사용 중일 때 (부분 일치 차단, §3.5 case C). */
public class EmailAlreadyUsedException extends BaseException {
    public EmailAlreadyUsedException() {
        super(EMAIL_ALREADY_USED.getStatus(), EMAIL_ALREADY_USED.getMessage());
    }
}
