package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;

/**
 * OAuth provider별 사용자 정보 조회 인터페이스
 * - 각 provider 구현체(KakaoApiClient, AppleApiClient 등)가 구현
 */
public interface OAuthApiClient {

    /** OAuth 사용자 정보 조회 */
    OAuthUserInfoDTO fetchUserInfo(String accessToken);
}
