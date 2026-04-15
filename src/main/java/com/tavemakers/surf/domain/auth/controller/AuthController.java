package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.auth.dto.response.KakaoLoginResult;
import com.tavemakers.surf.domain.auth.dto.response.LoginResDTO;
import com.tavemakers.surf.domain.auth.service.KakaoAuthService;
import com.tavemakers.surf.domain.auth.usecase.KakaoLoginUsecase;
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
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final KakaoLoginUsecase kakaoLoginUsecase;

    /**
     * 카카오 인가 화면으로 리다이렉트
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
     * 카카오 콜백 — 인가 코드 수신 후 로그인 처리
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
