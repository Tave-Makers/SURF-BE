package com.tavemakers.surf.domain.login.facade;

import com.tavemakers.surf.domain.login.auth.service.RefreshTokenService;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginFacade {
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        jwtService.extractRefreshToken(request).ifPresent(refreshToken -> {
            if (jwtService.isTokenValid(refreshToken)) {
                Long memberId = jwtService.extractMemberId(refreshToken).orElse(null);
                String deviceId = jwtService.extractDeviceId(refreshToken).orElse(null);
                if (memberId != null && deviceId != null) {
                    refreshTokenService.invalidate(memberId, deviceId);
                }
            }
        });

        jwtService.clearRefreshTokenCookie(response);
        SecurityContextHolder.clearContext();
    }
}
