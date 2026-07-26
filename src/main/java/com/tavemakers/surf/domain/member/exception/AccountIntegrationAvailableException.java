package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.global.common.exception.BaseException;
import lombok.Getter;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.ACCOUNT_INTEGRATION_AVAILABLE;

/** 기존 회원과의 계정 통합이 가능한 온보딩 요청임을 알린다. */
@Getter
public class AccountIntegrationAvailableException extends BaseException {

    // 통합 대기 정보 발급에 사용하는 감지 컨텍스트
    private final Long tempMemberId;
    private final Long socialAccountId;
    private final Long targetMemberId;
    private final Provider provider;
    private final String normalizedEmail;
    private final String normalizedPhone;

    // 통합 대기 정보 발급 후 채우는 응답 값
    private final String integrationToken;
    private final Long expiresInSeconds;
    private final String guideMessage;

    private AccountIntegrationAvailableException(Long tempMemberId, Long socialAccountId, Long targetMemberId,
                                                 Provider provider, String normalizedEmail, String normalizedPhone,
                                                 String integrationToken, Long expiresInSeconds, String guideMessage) {
        super(ACCOUNT_INTEGRATION_AVAILABLE.getStatus(), ACCOUNT_INTEGRATION_AVAILABLE.getMessage());
        this.tempMemberId = tempMemberId;
        this.socialAccountId = socialAccountId;
        this.targetMemberId = targetMemberId;
        this.provider = provider;
        this.normalizedEmail = normalizedEmail;
        this.normalizedPhone = normalizedPhone;
        this.integrationToken = integrationToken;
        this.expiresInSeconds = expiresInSeconds;
        this.guideMessage = guideMessage;
    }

    /** 통합 필요 감지 결과와 대기 정보 발급 컨텍스트를 생성한다. */
    public static AccountIntegrationAvailableException detected(Long tempMemberId, Long socialAccountId,
                                                                 Long targetMemberId, Provider provider,
                                                                 String normalizedEmail, String normalizedPhone) {
        return new AccountIntegrationAvailableException(
                tempMemberId, socialAccountId, targetMemberId, provider,
                normalizedEmail, normalizedPhone, null, null, null);
    }

    /** 발급된 통합 토큰과 안내 정보를 포함한 예외를 생성한다. */
    public static AccountIntegrationAvailableException issued(
            String integrationToken,
            Long expiresInSeconds,
            String guideMessage
    ) {
        return new AccountIntegrationAvailableException(
                null, null, null, null, null, null, integrationToken, expiresInSeconds, guideMessage);
    }
}
