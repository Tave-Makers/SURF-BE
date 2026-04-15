package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfo;

/**
 * OAuth provider별 사용자 정보 조회 인터페이스
 * - 각 provider 구현체(KakaoApiClient, AppleApiClient 등)가 구현
 */
public interface OAuthApiClient {

    /**
 * Fetches the OAuth user's information associated with the provided access token.
 *
 * @param accessToken the OAuth access token issued by the provider
 * @return an OAuthUserInfo populated with the provider's user data
 */
    OAuthUserInfo fetchUserInfo(String accessToken);
}
