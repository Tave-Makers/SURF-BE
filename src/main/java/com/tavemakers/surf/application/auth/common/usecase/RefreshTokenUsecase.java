package com.tavemakers.surf.application.auth.common.usecase;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.ClientType;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService.RotateResult;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.presentation.auth.common.dto.RefreshTokenResDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Refresh Token 회전과 새 Access Token 발급을 조합한다. */
@Service
@RequiredArgsConstructor
public class RefreshTokenUsecase {

    private final RefreshTokenService refreshTokenService;
    private final MemberGetService memberGetService;
    private final JwtService jwtService;

    public RefreshTokenResDTO refresh(HttpServletResponse response, ClientType clientType,
                                      String refreshToken, String origin) {
        RotateResult result = refreshTokenService.rotate(response, clientType, refreshToken, origin);
        Member member = memberGetService.getMember(result.memberId());
        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());

        return clientType == ClientType.APP
                ? RefreshTokenResDTO.app(accessToken, result.newRefreshToken())
                : RefreshTokenResDTO.web(accessToken);
    }
}
