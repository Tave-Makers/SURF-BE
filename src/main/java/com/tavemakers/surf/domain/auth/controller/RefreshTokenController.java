package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.auth.common.exception.TokenErrorMessage;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
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

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final MemberGetService memberGetService;

    /**
     * HttpOnly Refresh Token 쿠키로부터 새 Access Token을 발급하고 응답에 Refresh Token 회전 결과를 반영합니다.
     *
     * @param request  HTTP 요청 (Refresh Token 쿠키를 포함한 요청)
     * @param response HTTP 응답 (회전된 Refresh Token 쿠키를 설정함)
     * @return ApiResponse containing the newly issued access token under the key "accessToken".
     * @throws UnauthorizedException if the Refresh Token 쿠키가 요청에 존재하지 않을 경우
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
                .orElseThrow(() -> new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_MISSING.getMessage()));

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
    }
}