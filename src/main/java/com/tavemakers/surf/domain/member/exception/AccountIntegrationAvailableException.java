package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.global.common.exception.BaseException;
import lombok.Getter;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ACCOUNT_INTEGRATION_AVAILABLE;

/**
 * 온보딩 통합 이메일·전화번호가 모두 동일한 온보딩 완료(WAITING/APPROVED) 회원이 존재할 때 (§3.5 case B).
 * 검증기가 {@link #detected}로 감지 컨텍스트를 던지면, 온보딩 롤백 후 pending 발급을 거쳐 {@link #issued}로 재전파된다. (§3.6)
 */
@Getter
public class AccountIntegrationAvailableException extends BaseException {

    // 감지 컨텍스트 (case B) — PendingSocialIntegration 발급에 사용
    private final Long tempMemberId;
    private final Long socialAccountId;
    private final Provider provider;
    private final String normalizedEmail;
    private final String normalizedPhone;

    // 응답 페이로드 — pending 발급 후 채워짐(감지 단계에서는 null)
    private final String integrationToken;
    private final Long expiresInSeconds;
    private final String guideMessage;

    private AccountIntegrationAvailableException(Long tempMemberId, Long socialAccountId, Provider provider,
                                                String normalizedEmail, String normalizedPhone,
                                                String integrationToken, Long expiresInSeconds, String guideMessage) {
        super(ACCOUNT_INTEGRATION_AVAILABLE.getStatus(), ACCOUNT_INTEGRATION_AVAILABLE.getMessage());
        this.tempMemberId = tempMemberId;
        this.socialAccountId = socialAccountId;
        this.provider = provider;
        this.normalizedEmail = normalizedEmail;
        this.normalizedPhone = normalizedPhone;
        this.integrationToken = integrationToken;
        this.expiresInSeconds = expiresInSeconds;
        this.guideMessage = guideMessage;
    }

    /** 검증 단계 — 통합 필요 감지. pending 발급에 필요한 컨텍스트를 담는다. */
    public static AccountIntegrationAvailableException detected(Long tempMemberId, Long socialAccountId, Provider provider,
                                                               String normalizedEmail, String normalizedPhone) {
        return new AccountIntegrationAvailableException(
                tempMemberId, socialAccountId, provider, normalizedEmail, normalizedPhone, null, null, null);
    }

    /** pending 발급 후 — 응답 페이로드(토큰/만료/안내)를 담아 재전파한다. */
    public static AccountIntegrationAvailableException issued(String integrationToken, Long expiresInSeconds, String guideMessage) {
        return new AccountIntegrationAvailableException(
                null, null, null, null, null, integrationToken, expiresInSeconds, guideMessage);
    }
}
