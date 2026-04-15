package com.tavemakers.surf.domain.auth.kakao.controller;

import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoLoginResult;
import com.tavemakers.surf.domain.auth.kakao.service.KakaoAuthService;
import com.tavemakers.surf.domain.auth.kakao.usecase.KakaoLoginUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class KakaoLoginController {

    private final KakaoAuthService kakaoAuthService;
    private final KakaoLoginUsecase kakaoLoginUsecase;

    /**
     * Redirects the client to Kakao's authorization page to initiate OAuth login.
     *
     * @return a ResponseEntity with HTTP status 302 (Found) and the `Location` header set to the Kakao authorization URL.
     */
    @Operation(
            summary = "카카오 로그인 요청",
            description = "카카오 로그인 시작 시 카카오 인가 화면으로 리다이렉트(302) 합니다."
    )
    @GetMapping("/login/kakao")
    public ResponseEntity<Void> redirectToKakao() {
        String authorizeUrl = kakaoAuthService.buildAuthorizeUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizeUrl)
                .build();
    }

    /**
     * Handles Kakao OAuth callback and completes user login using the received authorization code.
     *
     * Processes the provided authorization `code`, issues authentication tokens, sets a refresh cookie,
     * and returns user login information wrapped in an ApiResponse.
     *
     * @param code the authorization code received from Kakao after user consent
     * @return a ResponseEntity containing an ApiResponse with LoginResDTO; includes a Set-Cookie header for the refresh token
     */
    @Operation(
            summary = "카카오 로그인 콜백",
            description = "인가 코드(code)를 받아 JWT AccessToken과 사용자 정보를 반환합니다."
    )
    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<ApiResponse<LoginResDTO>> kakaoCallback(
            @RequestParam("code") String code
    ) {
        log.info("[LOGIN][KAKAO][CALLBACK] start codeLength={}", code.length());

        KakaoLoginResult result = kakaoLoginUsecase.execute(code);

        log.info("[LOGIN][KAKAO][CALLBACK] success");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(ApiResponse.response(HttpStatus.OK, "로그인 성공", result.loginRes()));
    }
}
