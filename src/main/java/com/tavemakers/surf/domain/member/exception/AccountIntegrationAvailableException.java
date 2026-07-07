package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ACCOUNT_INTEGRATION_AVAILABLE;

/**
 * 온보딩 통합 이메일·전화번호가 모두 동일한 온보딩 완료(WAITING/APPROVED) 회원이 존재할 때 (§3.5 case B).
 * 통합 필요 감지 신호이며, 실제 계정 통합은 기존 계정 로그인 후 통합 API(Phase 4)에서 수행한다.
 */
public class AccountIntegrationAvailableException extends BaseException {
    public AccountIntegrationAvailableException() {
        super(ACCOUNT_INTEGRATION_AVAILABLE.getStatus(), ACCOUNT_INTEGRATION_AVAILABLE.getMessage());
    }
}
