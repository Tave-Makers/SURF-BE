package com.tavemakers.surf.application.auth.apple.usecase;

import com.tavemakers.surf.presentation.auth.apple.dto.AppleAppLoginReqDTO;
import com.tavemakers.surf.domain.auth.apple.dto.AppleTokenResDTO;
import com.tavemakers.surf.application.auth.apple.service.AppleAuthService;
import com.tavemakers.surf.application.auth.apple.service.AppleIdentityTokenVerifier;
import com.tavemakers.surf.application.auth.apple.service.AppleUserNameParser;
import com.tavemakers.surf.domain.auth.common.enums.ClientType;
import com.tavemakers.surf.presentation.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.application.auth.common.usecase.LoginTokenIssuer;
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
    private final AppleUserNameParser appleUserNameParser;
    private final MemberUpsertService memberUpsertService;
    private final LoginTokenIssuer loginTokenIssuer;

    /**
     * Apple Web 콜백 처리 (Authorization Code Flow + form_post).
     * @param code        Apple 발급 인가 코드
     * @param cookieNonce 쿠키에서 복원한 nonce 원문 (D9)
     * @param userPayload Apple {@code user} 폼 필드 원문 — 이름은 최초 인가 1회만 전달되므로 반드시 이 시점에 저장한다 (이슈 #392)
     */
    @Transactional
    public LoginPayloadResDTO executeWebCallback(String code, String cookieNonce, String userPayload, HttpServletRequest request) {
        appleAuthService.logCallback("apple");
        log.info("[LOGIN][APPLE][WEB] callback start");

        AppleTokenResDTO appleToken = appleAuthService.exchangeCodeForToken(code);
        String idToken = appleToken.idToken();
        log.info("[LOGIN][APPLE][WEB] token exchanged idToken={}...",
                idToken.substring(0, Math.min(idToken.length(), 10)));

        OAuthUserInfoDTO userInfo = identityTokenVerifier.verifyAndExtract(
                idToken, ClientType.WEB, cookieNonce
        );

        // id_token에는 name 클레임이 없다 — user 폼 필드에서 추출해 채운다
        userInfo = withName(userInfo, appleUserNameParser.extractName(userPayload));

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, userInfo);

        if (appleToken.refreshToken() != null) {
            updateAppleRefreshToken(member, appleToken.refreshToken());
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

        // Apple SDK는 이름을 최초 로그인 1회만 전달한다 — 이 시점에 저장하지 못하면 영구 유실 (이슈 #392)
        userInfo = withName(userInfo, req.name());

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, userInfo);

        // authorizationCode로 Apple refresh_token 교환 후 저장 — 탈퇴 시 /auth/revoke 호출에 사용
        if (req.authorizationCode() != null && !req.authorizationCode().isBlank()) {
            AppleTokenResDTO appleToken = appleAuthService.exchangeAppCodeForToken(req.authorizationCode());
            if (appleToken.refreshToken() != null) {
                updateAppleRefreshToken(member, appleToken.refreshToken());
                log.info("[LOGIN][APPLE][APP] refresh_token 저장 완료 memberId={}", member.getId());
            } else {
                log.warn("[LOGIN][APPLE][APP] Apple이 refresh_token 미반환 — 탈퇴 시 revoke 불가 memberId={}", member.getId());
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

    /** Apple refresh_token 을 SocialAccount(정규 저장소)에 저장한다 — 탈퇴 시 revoke 에 사용. */
    private void updateAppleRefreshToken(Member member, String refreshToken) {
        member.findSocialAccount(Provider.APPLE)
                .ifPresent(sa -> sa.updateAppleRefreshToken(refreshToken));
    }

    /** 이름이 있으면 nickname에 채워 반환 — 없으면(2회차 이후 로그인) 원본 유지 */
    private OAuthUserInfoDTO withName(OAuthUserInfoDTO userInfo, String name) {
        if (name == null || name.isBlank()) {
            return userInfo;
        }
        return new OAuthUserInfoDTO(userInfo.oauthId(), userInfo.email(), name.trim(), userInfo.profileImageUrl());
    }
}
