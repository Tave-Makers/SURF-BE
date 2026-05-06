package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
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

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class LogoutController {

    private static final String APP_REFRESH_HEADER = "X-Refresh-Token";

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그아웃. WEB=쿠키, APP={@code X-Refresh-Token} 헤더로 RefreshToken 수령 후 무효화.
     * <p>RefreshToken 누락이어도 idempotent — 200 반환.
     */
    @Operation(
            summary = "로그아웃",
            description = "WEB 은 refreshToken 쿠키, APP 은 X-Refresh-Token 헤더로 전달. 토큰 누락이어도 200."
    )
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            ClientType clientType
    ) {

        Optional<String> refreshToken = (clientType == ClientType.APP)
                ? Optional.ofNullable(request.getHeader(APP_REFRESH_HEADER))
                        .filter(s -> !s.isBlank())
                : jwtService.extractRefreshToken(request);

        refreshToken.ifPresent(token -> {
            if (jwtService.isTokenValid(token)) {
                Long memberId = jwtService.extractMemberId(token).orElse(null);
                String deviceId = jwtService.extractDeviceId(token).orElse(null);
                if (memberId != null && deviceId != null) {
                    refreshTokenService.invalidate(memberId, deviceId);
                }
            }
        });

        // WEB 만 쿠키 삭제. APP 은 클라이언트가 보관 중인 토큰 폐기.
        if (clientType != ClientType.APP) {
            jwtService.clearRefreshTokenCookie(response);
        }

        // 컨텍스트 정리
        SecurityContextHolder.clearContext();

        return ApiResponse.response(HttpStatus.NO_CONTENT, "로그아웃 완료", null);
    }
}
