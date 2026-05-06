package com.tavemakers.surf.domain.auth.apple.service;

import com.tavemakers.surf.domain.auth.apple.config.AppleOAuthProps;
import com.tavemakers.surf.domain.auth.apple.dto.AppleTokenResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/** Apple OAuth 공통 서비스 — 인가 URL 생성, 토큰 교환, 로깅 위임 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private final AppleApiClient appleApiClient;
    private final AppleAuthLogService appleAuthLogService;
    private final AppleOAuthProps props;

    /**
     * state + nonce 포함 Apple 인가 URL 생성.
     * <p>{@code response_mode=form_post} — 브라우저가 POST로 콜백을 전송한다 (D9 관련).
     */
    public String buildAuthorizeUrl(String state, String nonce) {
        appleAuthLogService.logAuthorize("apple", props.getRedirectUri());
        log.info("[APPLE][AUTHORIZE] start redirectUri={}", props.getRedirectUri());
        return UriComponentsBuilder
                .fromHttpUrl("https://appleid.apple.com/auth/authorize")
                .queryParam("client_id", props.getServiceClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "name email")
                .queryParam("response_mode", "form_post")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .build()
                .toUriString();
    }

    /** 인가 코드로 Apple 토큰 교환 */
    public AppleTokenResDTO exchangeCodeForToken(String code) {
        return appleApiClient.exchangeCodeForToken(code);
    }

    /** Apple 콜백 로그 */
    public void logCallback(String provider) {
        appleAuthLogService.logCallback(provider);
    }

    /** 로그인 성공 로그 */
    public void logLoginSuccess(Long userId, String issuedToken) {
        appleAuthLogService.logLoginSuccess(userId, issuedToken);
    }
}
