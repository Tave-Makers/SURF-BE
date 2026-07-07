package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.PHONE_ALREADY_USED;

/** 온보딩 전화번호가 이미 다른 회원에게 사용 중일 때 (부분 일치 차단, §3.5 case C). */
public class PhoneAlreadyUsedException extends BaseException {
    public PhoneAlreadyUsedException() {
        super(PHONE_ALREADY_USED.getStatus(), PHONE_ALREADY_USED.getMessage());
    }
}
