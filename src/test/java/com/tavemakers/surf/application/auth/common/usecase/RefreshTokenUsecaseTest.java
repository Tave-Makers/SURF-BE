package com.tavemakers.surf.application.auth.common.usecase;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.ClientType;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService.RotateResult;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.presentation.auth.common.dto.RefreshTokenResDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenUsecaseTest {

    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final MemberGetService memberGetService = mock(MemberGetService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final RefreshTokenUsecase usecase =
            new RefreshTokenUsecase(refreshTokenService, memberGetService, jwtService);

    @BeforeEach
    void setUp() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(memberGetService.getMember(1L)).thenReturn(member);
        when(jwtService.createAccessToken(1L, "MEMBER")).thenReturn("new-access");
    }

    @Test
    void appResponseContainsRotatedRefreshToken() {
        when(refreshTokenService.rotate(response, ClientType.APP, "old-refresh", "csr"))
                .thenReturn(new RotateResult(1L, "new-refresh"));

        RefreshTokenResDTO result = usecase.refresh(response, ClientType.APP, "old-refresh", "csr");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).rotate(response, ClientType.APP, "old-refresh", "csr");
    }

    @Test
    void webResponseExcludesRefreshTokenFromBody() {
        when(refreshTokenService.rotate(response, ClientType.WEB, "old-refresh", "rsc"))
                .thenReturn(new RotateResult(1L, null));

        RefreshTokenResDTO result = usecase.refresh(response, ClientType.WEB, "old-refresh", "rsc");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isNull();
        verify(refreshTokenService).rotate(response, ClientType.WEB, "old-refresh", "rsc");
    }
}
