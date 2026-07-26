package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.INTEGRATION_NOT_ELIGIBLE;

/**
 * 통합 조건 불충족(email·phone 불일치, 상태 부적합, 대상 SocialAccount 이전/부재 등) 시 (§3.6).
 * 어느 조건인지는 노출하지 않는다(계정 정보 유추 방지).
 */
public class IntegrationNotEligibleException extends BaseException {
    /** 통합 조건 불충족 예외를 생성한다. */
    public IntegrationNotEligibleException() {
        super(INTEGRATION_NOT_ELIGIBLE.getStatus(), INTEGRATION_NOT_ELIGIBLE.getMessage());
    }
}
