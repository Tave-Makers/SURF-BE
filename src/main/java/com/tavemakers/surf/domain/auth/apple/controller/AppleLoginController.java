package com.tavemakers.surf.domain.auth.apple.controller;

import com.tavemakers.surf.domain.auth.apple.dto.AppleAppLoginReqDTO;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import com.tavemakers.surf.domain.auth.apple.service.AppleAuthService;
import com.tavemakers.surf.domain.auth.apple.service.AppleOAuthStateService;
import com.tavemakers.surf.domain.auth.apple.usecase.AppleLoginUsecase;
import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class AppleLoginController {

    private final AppleAuthService appleAuthService;
    private final AppleLoginUsecase appleLoginUsecase;
    private final AppleOAuthStateService appleOAuthStateService;

    /**
     * Apple 로그인 요청 — state/nonce 생성 후 Redis 저장, Apple 인가 화면으로 302 리다이렉트.
     * <p>state는 CSRF 방지, nonce는 identityToken 재사용 방지용. form_post cross-site 쿠키 차단 대응으로 Redis 보관.
     */
    @Operation(
            summary = "Apple 로그인 요청",
            description = "state/nonce를 생성해 Redis에 저장하고 Apple 인가 화면으로 302 리다이렉트합니다."
    )
    @GetMapping("/login/apple")
    public ResponseEntity<Void> redirectToApple() {
        String state = generateSecureRandom();
        String nonce = generateSecureRandom();

        appleOAuthStateService.save(state, nonce);

        String authorizeUrl = appleAuthService.buildAuthorizeUrl(state, nonce);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizeUrl)
                .build();
    }

    /**
     * Apple Web 로그인 콜백 (form_post).
     * <p>Redis에서 state로 nonce 조회(1회용) → 인가 코드 교환 → identityToken 검증 → JWT 발급.
     * RefreshToken 은 HttpOnly 쿠키로 전달 (WEB 흐름).
     */
    @Operation(
            summary = "Apple Web 로그인 콜백",
            description = "Apple form_post 콜백. Redis state 검증 후 JWT AccessToken + Set-Cookie(refreshToken) 반환."
    )
    @PostMapping("/login/oauth2/code/apple")
    public ResponseEntity<ApiResponse<LoginResDTO>> appleWebCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state") String state,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response
    ) {
        if (error != null) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_AUTH_CALLBACK_ERROR.getStatus(),
                    AppleAuthErrorMessage.APPLE_AUTH_CALLBACK_ERROR.getMessage()
            );
        }

        // code 검증을 nonce 소비 전에 수행 — nonce는 1회용이므로 code 없이 소비되면 재시도 불가
        if (code == null || code.isBlank()) {
            throw new AppleAuthException(
                    AppleAuthErrorMessage.MISSING_AUTHORIZATION_CODE.getStatus(),
                    AppleAuthErrorMessage.MISSING_AUTHORIZATION_CODE.getMessage()
            );
        }

        log.info("[LOGIN][APPLE][WEB][CALLBACK] start state={}", state);

        // state로 nonce를 원자적 조회·삭제 (1회용). state 없거나 만료 시 INVALID_STATE 예외 발생
        String nonce = appleOAuthStateService.popNonce(state);

        LoginPayloadResDTO payload = appleLoginUsecase.executeWebCallback(code, nonce);

        log.info("[LOGIN][APPLE][WEB][CALLBACK] success");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, payload.refreshCookie().toString())
                .body(ApiResponse.response(HttpStatus.OK, "로그인 성공", payload.loginRes()));
    }

    /**
     * Apple SDK 앱 로그인 — identityToken + nonce 검증 후 JWT 발급.
     * <p>APP 클라이언트는 응답 본문에 {@code loginRes} + {@code refreshToken} 수신.
     */
    @Operation(
            summary = "Apple 앱 SDK 로그인",
            description = "Apple SDK identityToken을 RS256 검증 후 SURF JWT 발급. 응답 본문에 refreshToken 포함 (APP 흐름)."
    )
    @PostMapping("/login/apple/app")
    public ApiResponse<LoginPayloadResDTO> appleAppLogin(
            @RequestBody @Valid AppleAppLoginReqDTO req,
            ClientType clientType
    ) {
        LoginPayloadResDTO payload = appleLoginUsecase.executeAppLogin(req, clientType);
        return ApiResponse.response(HttpStatus.OK, "로그인 성공", payload);
    }

    /**
     * CSRF/Replay 공격 방지용 state/nonce 생성. 128-bit secure random → Base64 URL-safe 인코딩.
     * @return URL-safe한 22자 길이의 문자열 (16바이트 → Base64 인코딩 시 22문자, 패딩 제거)
     */
    private String generateSecureRandom() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
