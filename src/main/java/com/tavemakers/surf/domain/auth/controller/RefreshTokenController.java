package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.RefreshTokenResDTO;
import com.tavemakers.surf.domain.auth.common.exception.TokenErrorMessage;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService.RotateResult;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class RefreshTokenController {

    private static final String APP_REFRESH_HEADER = "X-Refresh-Token";

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final MemberGetService memberGetService;

    /**
     * Refresh Token 기반 Access Token 재발급.
     * <p>WEB: HttpOnly 쿠키 → 쿠키 회전. APP: {@code X-Refresh-Token} 헤더 → 본문 회전.
     */
    @Operation(
            summary = "Access Token 재발급",
            description = "WEB 은 refreshToken 쿠키, APP 은 X-Refresh-Token 헤더로 전달합니다."
    )
    @PostMapping("/auth/refresh")
    public ApiResponse<RefreshTokenResDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            ClientType clientType
    ) {
        // 1) 클라이언트 타입별 RefreshToken 추출
        String refreshToken = extractRefreshToken(request, clientType);

        // 2) RTR: 검증 + 회전 (WEB=쿠키 부착, APP=새 토큰 반환)
        RotateResult result = refreshTokenService.rotate(response, clientType, refreshToken);

        // 3) 새 access 발급
        Member member = memberGetService.getMember(result.memberId());
        String newAccessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());

        // 4) 클라이언트 타입별 응답 본문 조립
        RefreshTokenResDTO body = (clientType == ClientType.APP)
                ? RefreshTokenResDTO.app(newAccessToken, result.newRefreshToken())
                : RefreshTokenResDTO.web(newAccessToken);

        return ApiResponse.response(HttpStatus.OK, "Access token 재발급 성공", body);
    }

    private String extractRefreshToken(HttpServletRequest request, ClientType clientType) {
        if (clientType == ClientType.APP) {
            String header = request.getHeader(APP_REFRESH_HEADER);
            if (header == null || header.isBlank()) {
                throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_MISSING.getMessage());
            }
            return header;
        }
        return jwtService.extractRefreshToken(request)
                .orElseThrow(() -> new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_MISSING.getMessage()));
    }
}
