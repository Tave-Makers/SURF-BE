package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.login.auth.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
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

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증 - 토큰 재발급")
public class AuthRefreshController {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final MemberGetService memberGetService;

    /**
     * Refresh Token 기반 Access Token 재발급
     */
    @Operation(
            summary = "Access Token 재발급",
            description = "HttpOnly Refresh Token 쿠키를 이용해 Access Token을 재발급합니다."
    )
    @PostMapping("/auth/refresh")
    public ApiResponse<Map<String, String>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1) refresh 쿠키 추출
        String refreshToken = jwtService.extractRefreshToken(request)
                .orElse(null);

        if (refreshToken == null) {
            return ApiResponse.response(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token이 없습니다.",
                    null
            );
        }

        try {
            // 2) RTR: 검증 + 회전 (여기서 새 refresh 쿠키 세팅됨)
            Long memberId = refreshTokenService.rotate(response, refreshToken);

            Member member = memberGetService.getMember(memberId);

            // 3) 새 access 발급
            String newAccessToken = jwtService.createAccessToken(
                    member.getId(),
                    member.getRole().name()
            );

            // 4) access 반환
            return ApiResponse.response(
                    HttpStatus.OK,
                    "Access token 재발급 성공",
                    Map.of("accessToken", newAccessToken)
            );

        } catch (Exception e) {
            return ApiResponse.response(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token이 유효하지 않습니다.",
                    null
            );
        }
    }
}