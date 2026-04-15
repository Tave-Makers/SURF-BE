package com.tavemakers.surf.domain.auth.kakao.usecase;

import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfo;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoLoginResult;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoTokenResDTO;
import com.tavemakers.surf.domain.auth.kakao.service.KakaoAuthService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberUpsertService;
import com.tavemakers.surf.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginUsecase {

    private final KakaoAuthService kakaoAuthService;
    private final MemberUpsertService memberUpsertService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /** 카카오 인가 코드로 로그인 처리 후 결과 반환 */
    public KakaoLoginResult execute(String code) {

        // 1. 콜백 진입 로그
        kakaoAuthService.logCallback("kakao", code.length());

        // 2. 인가 코드 → 카카오 토큰
        KakaoTokenResDTO token = kakaoAuthService.exchangeCodeForToken(code);

        // 3. 카카오 사용자 정보 조회 (OAuthUserInfo로 반환)
        OAuthUserInfo userInfo = kakaoAuthService.getUserInfo(token.accessToken());

        // 4. 회원 upsert
        Member member = memberUpsertService.upsertRegisteringFromKakao(userInfo);

        // 5. deviceId 생성
        String deviceId = UUID.randomUUID().toString();

        // 6. AccessToken 발급
        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());

        // 7. RefreshToken 발급 및 쿠키 반환
        ResponseCookie refreshCookie = refreshTokenService.issue(member.getId(), deviceId);

        // 8. 로그인 성공 로그
        kakaoAuthService.logLoginSuccess(member.getId(), accessToken.substring(0, Math.min(accessToken.length(), 10)) + "...");

        // 9. 응답 DTO 조립
        LoginResDTO loginRes = LoginResDTO.of(
                userInfo.nickname(),
                userInfo.email(),
                accessToken,
                userInfo.profileImageUrl()
        );

        return new KakaoLoginResult(loginRes, refreshCookie);
    }
}
