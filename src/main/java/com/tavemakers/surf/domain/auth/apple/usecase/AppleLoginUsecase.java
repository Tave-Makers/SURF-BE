package com.tavemakers.surf.domain.auth.apple.usecase;

import com.tavemakers.surf.domain.auth.apple.dto.AppleAppLoginReqDTO;
import com.tavemakers.surf.domain.auth.apple.dto.AppleTokenResDTO;
import com.tavemakers.surf.domain.auth.apple.service.AppleAuthService;
import com.tavemakers.surf.domain.auth.apple.service.AppleIdentityTokenVerifier;
import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.auth.common.service.LoginTokenIssuer;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberUpsertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Apple Web/App 로그인 복합 비즈니스 로직 조합 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleLoginUsecase {

    private final AppleAuthService appleAuthService;
    private final AppleIdentityTokenVerifier identityTokenVerifier;
    private final MemberUpsertService memberUpsertService;
    private final LoginTokenIssuer loginTokenIssuer;

    /**
     * Apple Web 콜백 처리 (Authorization Code Flow + form_post).
     * @param code        Apple 발급 인가 코드
     * @param cookieNonce 쿠키에서 복원한 nonce 원문 (D9)
     */
    @Transactional
    public LoginPayloadResDTO executeWebCallback(String code, String cookieNonce, HttpServletRequest request) {
        appleAuthService.logCallback("apple");
        log.info("[LOGIN][APPLE][WEB] callback start");

        AppleTokenResDTO appleToken = appleAuthService.exchangeCodeForToken(code);
        String idToken = appleToken.idToken();
        log.info("[LOGIN][APPLE][WEB] token exchanged idToken={}...",
                idToken.substring(0, Math.min(idToken.length(), 10)));

        OAuthUserInfoDTO userInfo = identityTokenVerifier.verifyAndExtract(
                idToken, ClientType.WEB, cookieNonce
        );

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, userInfo);

        if (appleToken.refreshToken() != null) {
            member.updateAppleRefreshToken(appleToken.refreshToken());
        }
        LoginPayloadResDTO payload = loginTokenIssuer.issue(member, userInfo, ClientType.WEB, request);

        String accessToken = payload.loginRes().accessToken();
        appleAuthService.logLoginSuccess(
                member.getId(),
                accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..."
        );

        return payload;
    }

    /**
     * Apple SDK 앱 로그인 처리 (iOS identityToken 직접 검증).
     * @param req        identityToken + nonce 원문 (+ name 최초 로그인 시 + authorizationCode)
     * @param clientType resolver 주입 — APP=본문 RefreshToken 전달
     */
    @Transactional
    public LoginPayloadResDTO executeAppLogin(AppleAppLoginReqDTO req, ClientType clientType, HttpServletRequest request) {
        String masked = req.identityToken().substring(0, Math.min(req.identityToken().length(), 10)) + "...";
        log.info("[LOGIN][APPLE][APP] start identityToken={}", masked);

        OAuthUserInfoDTO userInfo = identityTokenVerifier.verifyAndExtract(
                req.identityToken(), clientType, req.nonce()
        );

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, userInfo);

        // authorizationCode로 Apple refresh_token 교환 후 저장 — 탈퇴 시 /auth/revoke 호출에 사용
        if (req.authorizationCode() != null && !req.authorizationCode().isBlank()) {
            AppleTokenResDTO appleToken = appleAuthService.exchangeCodeForToken(req.authorizationCode());
            if (appleToken.refreshToken() != null) {
                member.updateAppleRefreshToken(appleToken.refreshToken());
                log.info("[LOGIN][APPLE][APP] refresh_token 저장 완료 memberId={}", member.getId());
            }
        } else {
            log.warn("[LOGIN][APPLE][APP] authorizationCode 미전달 — 탈퇴 시 revoke 불가 memberId={}", member.getId());
        }

        LoginPayloadResDTO payload = loginTokenIssuer.issue(member, userInfo, clientType, request);

        String accessToken = payload.loginRes().accessToken();
        appleAuthService.logLoginSuccess(
                member.getId(),
                accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..."
        );

        return payload;
    }
}
