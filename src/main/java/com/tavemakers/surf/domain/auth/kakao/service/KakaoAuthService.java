package com.tavemakers.surf.domain.auth.kakao.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
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

    /** 인가 URL 생성 */
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

    /** 카카오 콜백 로그 */
    public void logCallback(String provider, int codeLength) {
        kakaoAuthLogService.logCallback(provider, codeLength);
    }

    /** 인가 코드로 토큰 교환 */
    public KakaoTokenResDTO exchangeCodeForToken(String code) {
        return kakaoApiClient.exchangeCodeForToken(code);
    }

    /** AccessToken으로 공통 사용자 정보 요청 */
    public OAuthUserInfoDTO getUserInfo(String accessToken) {
        return kakaoApiClient.fetchUserInfo(accessToken);
    }

    /** 로그인 성공 로그 */
    public void logLoginSuccess(Long userId, String issuedToken) {
        kakaoAuthLogService.logLoginSuccess(userId, issuedToken);
    }
}
