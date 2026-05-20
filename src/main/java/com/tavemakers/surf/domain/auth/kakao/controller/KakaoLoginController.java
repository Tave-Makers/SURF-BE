package com.tavemakers.surf.domain.auth.kakao.controller;

import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.domain.auth.kakao.exception.KakaoAuthErrorMessage;
import com.tavemakers.surf.domain.auth.kakao.exception.KakaoAuthException;
import com.tavemakers.surf.domain.auth.kakao.service.KakaoAuthService;
import com.tavemakers.surf.domain.auth.kakao.usecase.KakaoLoginUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class KakaoLoginController {

    private final KakaoAuthService kakaoAuthService;
    private final KakaoLoginUsecase kakaoLoginUsecase;

    /** CSRF 방지용 state를 쿠키에 저장한 뒤 카카오 인가 화면으로 리다이렉트한다. */
    @Operation(
            summary = "카카오 로그인 요청",
            description = "카카오 로그인 시작 시 카카오 인가 화면으로 리다이렉트(302) 합니다."
    )
    @GetMapping("/login/kakao")
    public ResponseEntity<Void> redirectToKakao() {
        String state = generateState();
        ResponseCookie stateCookie = ResponseCookie.from("oauth_state", state)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/login/oauth2/code/kakao")
                .maxAge(300)
                .build();
        String authorizeUrl = kakaoAuthService.buildAuthorizeUrl(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .header(HttpHeaders.LOCATION, authorizeUrl)
                .build();
    }

    /** 카카오 콜백 — state 검증 및 인가 코드 수신 후 로그인 처리 */
    @Operation(
            summary = "카카오 로그인 콜백",
            description = "인가 코드(code)를 받아 JWT AccessToken과 사용자 정보를 반환합니다."
    )
    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<ApiResponse<LoginResDTO>> kakaoCallback(
            @RequestParam(value = "state", required = true) String state,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @CookieValue(value = "oauth_state", required = false) String storedState,
            HttpServletRequest request
    ) {
        try {
            if (storedState == null || !storedState.equals(state)) {
                throw new KakaoAuthException(
                        KakaoAuthErrorMessage.INVALID_STATE.getStatus(),
                        KakaoAuthErrorMessage.INVALID_STATE.getMessage()
                );
            }
            if (error != null) {
                throw new KakaoAuthException(
                        KakaoAuthErrorMessage.KAKAO_AUTH_CALLBACK_ERROR.getStatus(),
                        KakaoAuthErrorMessage.KAKAO_AUTH_CALLBACK_ERROR.getMessage()
                );
            }

            log.info("[LOGIN][KAKAO][CALLBACK] start codeLength={}", code.length());

            LoginPayloadResDTO payload = kakaoLoginUsecase.execute(code, request);

            log.info("[LOGIN][KAKAO][CALLBACK] success");
            return payload.toWebResponseBuilder()
                    .body(ApiResponse.response(HttpStatus.OK, "로그인 성공", payload.loginRes()));
        } catch (Exception e) {
            kakaoAuthService.logLoginFailed(resolveStatusCode(e), e.getClass().getSimpleName());
            throw e;
        }
    }

    private int resolveStatusCode(Exception e) {
        if (e instanceof KakaoAuthException kakaoAuthException) {
            return kakaoAuthException.getStatus().value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
