package com.tavemakers.surf.domain.auth.kakao.usecase;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.auth.common.service.LoginTokenIssuer;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoTokenResDTO;
import com.tavemakers.surf.domain.auth.kakao.service.KakaoAuthService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberUpsertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginUsecase {

    private final KakaoAuthService kakaoAuthService;
    private final MemberUpsertService memberUpsertService;
    private final LoginTokenIssuer loginTokenIssuer;

    /** 카카오 인가 코드로 Web 로그인 처리 후 응답 payload 반환. App SDK 흐름은 별도 메서드로 추가될 예정(Step 4). */
    @Transactional
    public LoginPayloadResDTO execute(String code) {

        // 1. 콜백 진입 로그
        kakaoAuthService.logCallback("kakao", code.length());

        // 2. 인가 코드 → 카카오 토큰
        KakaoTokenResDTO token = kakaoAuthService.exchangeCodeForToken(code);

        // 3. 카카오 사용자 정보 조회 (OAuthUserInfoDTO로 반환)
        OAuthUserInfoDTO userInfo = kakaoAuthService.getUserInfo(token.accessToken());

        // 4. 회원 upsert
        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, userInfo);

        // 5. JWT 발급 + 응답 wrapper 조립 (Web=쿠키)
        LoginPayloadResDTO payload = loginTokenIssuer.issue(member, userInfo, ClientType.WEB);

        // 6. 로그인 성공 로그
        String accessToken = payload.loginRes().accessToken();
        kakaoAuthService.logLoginSuccess(
                member.getId(),
                accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..."
        );

        return payload;
    }
}
