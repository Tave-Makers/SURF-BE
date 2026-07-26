package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INTEGRATION_TOKEN_EXPIRED;

/** 통합 토큰이 만료(expiresAt 경과)됐을 때 (§3.6, TTL 30분). */
public class PendingIntegrationExpiredException extends BaseException {
    /** 만료된 통합 토큰 예외를 생성한다. */
    public PendingIntegrationExpiredException() {
        super(INTEGRATION_TOKEN_EXPIRED.getStatus(), INTEGRATION_TOKEN_EXPIRED.getMessage());
    }
}
