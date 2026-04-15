package com.tavemakers.surf.domain.auth.usecase;

import com.tavemakers.surf.domain.auth.dto.response.KakaoLoginResult;
import com.tavemakers.surf.domain.auth.dto.response.KakaoTokenResDTO;
import com.tavemakers.surf.domain.auth.dto.response.KakaoUserInfoDTO;
import com.tavemakers.surf.domain.auth.dto.response.LoginResDTO;
import com.tavemakers.surf.domain.auth.service.KakaoAuthService;
import com.tavemakers.surf.domain.auth.service.RefreshTokenService;
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

        // 3. 카카오 사용자 정보 조회
        KakaoUserInfoDTO userInfo = kakaoAuthService.getUserInfo(token.accessToken());

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
        var account = userInfo.kakaoAccount();
        LoginResDTO loginRes = LoginResDTO.of(
                account.profile().nickname(),
                account.email(),
                accessToken,
                account.profile().profileImageUrl()
        );

        return new KakaoLoginResult(loginRes, refreshCookie);
    }
}
