package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INTEGRATION_TOKEN_NOT_FOUND;

/** 통합 토큰으로 대기 row를 찾지 못할 때 (부재 = 이미 사용/무효, §3.6). */
public class PendingIntegrationNotFoundException extends BaseException {
    /** 유효하지 않은 통합 토큰 예외를 생성한다. */
    public PendingIntegrationNotFoundException() {
        super(INTEGRATION_TOKEN_NOT_FOUND.getStatus(), INTEGRATION_TOKEN_NOT_FOUND.getMessage());
    }
}
