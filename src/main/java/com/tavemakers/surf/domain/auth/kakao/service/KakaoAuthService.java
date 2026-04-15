package com.tavemakers.surf.domain.auth.kakao.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfo;
import com.tavemakers.surf.domain.auth.kakao.config.KakaoOAuthProps;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoTokenResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoApiClient kakaoApiClient;
    private final KakaoAuthLogService kakaoAuthLogService;
    private final KakaoOAuthProps props;

    /**
     * Builds the Kakao OAuth 2.0 authorization URL with required query parameters.
     *
     * Also records the authorization attempt using the Kakao auth log service.
     *
     * @return the authorization URL string containing `response_type=code`, `client_id`, `redirect_uri`, and `scope=account_email profile_nickname profile_image`
     */
    public String buildAuthorizeUrl() {
        kakaoAuthLogService.logAuthorize("kakao", props.getRedirectUri());
        log.info("[KAKAO][AUTHORIZE] start redirectUri={}", props.getRedirectUri());
        return UriComponentsBuilder
                .fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("scope", "account_email profile_nickname profile_image")
                .build()
                .toUriString();
    }

    /**
     * Record callback metadata for the specified OAuth provider.
     *
     * @param provider  the OAuth provider identifier (e.g., "kakao")
     * @param codeLength the length in characters of the authorization code received in the callback
     */
    public void logCallback(String provider, int codeLength) {
        kakaoAuthLogService.logCallback(provider, codeLength);
    }

    /**
     * Exchange an authorization code for a Kakao token response.
     *
     * @param code the authorization code received from Kakao after user consent
     * @return a KakaoTokenResDTO containing the issued access (and refresh) token details
     */
    public KakaoTokenResDTO exchangeCodeForToken(String code) {
        return kakaoApiClient.exchangeCodeForToken(code);
    }

    /**
     * Fetches the common user information associated with the given Kakao access token.
     *
     * @param accessToken the Kakao OAuth access token
     * @return an OAuthUserInfo containing the user's profile and account information
     */
    public OAuthUserInfo getUserInfo(String accessToken) {
        return kakaoApiClient.fetchUserInfo(accessToken);
    }

    /**
     * Record a successful login event for the specified user using the issued token.
     *
     * @param userId the identifier of the user who logged in
     * @param issuedToken the token issued to the user at login
     */
    public void logLoginSuccess(Long userId, String issuedToken) {
        kakaoAuthLogService.logLoginSuccess(userId, issuedToken);
    }
}
