package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 로그인 성공 후 SURF JWT 발급 + 응답 wrapper 조립 일원화.
 * <p>4조합(Kakao/Apple × Web/App) 모두 본 클래스를 경유한다. Usecase 는 ClientType 만 넘기고 분기 로직을 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class LoginTokenIssuer {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /** 회원 + OAuth 사용자 정보 + 클라이언트 타입 → 토큰 발급 후 응답 wrapper 반환. */
    public LoginPayloadResDTO issue(Member member, OAuthUserInfoDTO info, ClientType clientType) {
        String deviceId = UUID.randomUUID().toString();
        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());
        LoginResDTO loginRes = buildLoginRes(info, accessToken);

        if (clientType == ClientType.APP) {
            String refreshToken = refreshTokenService.issueRaw(member.getId(), deviceId);
            return LoginPayloadResDTO.app(loginRes, refreshToken);
        }
        ResponseCookie cookie = refreshTokenService.issue(member.getId(), deviceId);
        return LoginPayloadResDTO.web(loginRes, cookie);
    }

    private LoginResDTO buildLoginRes(OAuthUserInfoDTO info, String accessToken) {
        return LoginResDTO.of(info.nickname(), info.email(), accessToken, info.profileImageUrl());
    }
}
