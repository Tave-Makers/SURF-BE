package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class LogoutController {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "로그아웃", description = "현재 디바이스의 refreshToken을 무효화하고 쿠키를 삭제합니다.")
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        jwtService.extractRefreshToken(request).ifPresent(refreshToken -> {
            if (jwtService.isTokenValid(refreshToken)) {
                Long memberId = jwtService.extractMemberId(refreshToken).orElse(null);
                String deviceId = jwtService.extractDeviceId(refreshToken).orElse(null);
                if (memberId != null && deviceId != null) {
                    refreshTokenService.invalidate(memberId, deviceId);
                }
            }
        });

        // 쿠키 삭제는 항상 수행
        jwtService.clearRefreshTokenCookie(response);

        // 컨텍스트 정리
        SecurityContextHolder.clearContext();

        return ApiResponse.response(HttpStatus.NO_CONTENT, "로그아웃 완료", null);
    }
}