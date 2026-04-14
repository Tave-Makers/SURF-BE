package com.tavemakers.surf.domain.auth.controller;

import com.tavemakers.surf.domain.login.LoginResDTO;
import com.tavemakers.surf.domain.login.auth.service.RefreshTokenService;
import com.tavemakers.surf.domain.login.kakao.dto.KakaoTokenResDTO;
import com.tavemakers.surf.domain.login.kakao.dto.KakaoUserInfoDTO;
import com.tavemakers.surf.domain.auth.service.KakaoAuthService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberUpsertService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "소셜 로그인 관련", description = "이 명세서를 통해서는 아무런 결과는 안나옵니다..")
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    private final MemberUpsertService memberUpsertService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 1) 카카오 인가 화면으로 리다이렉트
     * event: login.kakao.request
     */
    @Operation(summary = "카카오 인가 화면으로 리다이렉트")
    @GetMapping("/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {

        String authorizeUrl = kakaoAuthService.buildAuthorizeUrl();

        log.info("[LOGIN][KAKAO][AUTHORIZE] redirect to kakao");

        // 3) 카카오로 보내기
        response.sendRedirect(authorizeUrl);
    }

    /**
     * 2) 카카오 콜백 (Redirect URI와 동일 경로)
     *    - authCode → accessToken → userInfo 처리
     *    - refreshToken은 HttpOnly 쿠키로 전달
     * event: login.kakao.callback
     */
    @Operation(
            summary = "카카오 로그인 콜백",
            description = "인가 코드(code)를 받아 JWT AccessToken과 사용자 정보를 반환합니다."
    )
    @GetMapping("/login/oauth2/code/kakao")
    public ApiResponse<LoginResDTO> kakaoCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) {
        log.info("[LOGIN][KAKAO][CALLBACK] start codeLength={}",
                code != null ? code.length() : null);

        if (code == null || code.isBlank()) {
            return ApiResponse.response(HttpStatus.BAD_REQUEST, "인가 코드가 없습니다.", null);
        }
        try {
            // 1. 콜백 진입
            kakaoAuthService.logCallback("kakao", code.length());

            // 2. 인가 코드 → 토큰
            KakaoTokenResDTO token = kakaoAuthService.exchangeCodeForToken(code);
            log.info("[LOGIN][KAKAO] exchange token success accessTokenLength={}",
                    token.accessToken() != null ? token.accessToken().length() : null);

            // 3. 사용자 정보 조회
            KakaoUserInfoDTO userInfo = kakaoAuthService.getUserInfo(token.accessToken());

            // 4. 회원 upsert
            Member member = memberUpsertService.upsertRegisteringFromKakao(userInfo);

            // 5. deviceId 생성 (기기 식별자)
            String deviceId = UUID.randomUUID().toString();

            // 6. AccessToken 발급
            String accessToken =
                    jwtService.createAccessToken(
                            member.getId(),
                            member.getRole().name()
                    );

            // 7. RefreshToken 발급
            refreshTokenService.issue(response, member.getId(), deviceId);

            // 8. SecurityContext 설정
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    member.getEmail(),
                    null,
                    List.of(new SimpleGrantedAuthority(member.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            kakaoAuthService.logLoginSuccess(member.getId(), accessToken.substring(0, 10) + "...");

            var account = userInfo.kakaoAccount();
            LoginResDTO loginRes = LoginResDTO.of(
                    account.profile().nickname(),
                    account.email(),
                    accessToken,
                    account.profile().profileImageUrl()
            );

            log.info("[LOGIN][KAKAO][CALLBACK] success");
            return ApiResponse.response(HttpStatus.OK, "로그인 성공", loginRes);

        } catch (Exception e) {
            log.error("[LOGIN][KAKAO][CALLBACK] failed", e);
            try {
                kakaoAuthService.logLoginFailed(
                        401,
                        e.getMessage() != null ? e.getMessage() : "카카오 로그인 실패"
                );
            } catch (Exception ignored) {
                // AOP가 ERROR로 인식하도록 예외는 던지되, 여기서 무시함
            }

            return ApiResponse.response(HttpStatus.UNAUTHORIZED, "로그인 실패", null);
        }

    }
}
