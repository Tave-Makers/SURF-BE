package com.tavemakers.surf.application.auth.apple.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.auth.apple.service.AppleAuthService;
import com.tavemakers.surf.application.auth.apple.service.AppleIdentityTokenVerifier;
import com.tavemakers.surf.application.auth.apple.service.AppleUserNameParser;
import com.tavemakers.surf.application.auth.common.usecase.LoginTokenIssuer;
import com.tavemakers.surf.domain.auth.apple.dto.AppleTokenResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.ClientType;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.service.MemberUpsertService;
import com.tavemakers.surf.presentation.auth.apple.dto.AppleAppLoginReqDTO;
import com.tavemakers.surf.presentation.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.presentation.auth.common.dto.LoginResDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Apple 로그인 이름 배선 회귀 테스트 (이슈 #392) —
 * Apple이 최초 인가 1회만 주는 이름이 upsert까지 도달하는지 검증한다.
 * 여기서 유실되면 그 계정은 영구적으로 이름을 받을 수 없다.
 */
@ExtendWith(MockitoExtension.class)
class AppleLoginUsecaseNameTest {

    private static final String ID_TOKEN = "identity-token-raw-value";
    private static final String NONCE = "nonce-raw";
    private static final OAuthUserInfoDTO TOKEN_INFO =
            new OAuthUserInfoDTO("apple-sub-001", "relay@privaterelay.appleid.com", null, null);

    @Mock
    private AppleAuthService appleAuthService;
    @Mock
    private AppleIdentityTokenVerifier identityTokenVerifier;
    @Spy
    private AppleUserNameParser appleUserNameParser = new AppleUserNameParser(new ObjectMapper());
    @Mock
    private MemberUpsertService memberUpsertService;
    @Mock
    private LoginTokenIssuer loginTokenIssuer;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AppleLoginUsecase appleLoginUsecase;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .name(null)
                .status(MemberStatus.REGISTERING)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();

        LoginPayloadResDTO payload = LoginPayloadResDTO.app(
                LoginResDTO.builder().accessToken("access-token-value").build());
        given(loginTokenIssuer.issue(any(), any(), any(), any())).willReturn(payload);
        given(memberUpsertService.upsertRegisteringFromOAuth(eq(Provider.APPLE), any())).willReturn(member);
    }

    @Test
    @DisplayName("앱 로그인 — 최초 로그인의 name이 upsert의 nickname으로 전달된다")
    void appLogin_passesNameToUpsert() {
        given(identityTokenVerifier.verifyAndExtract(ID_TOKEN, ClientType.APP, NONCE)).willReturn(TOKEN_INFO);
        AppleAppLoginReqDTO req = new AppleAppLoginReqDTO(ID_TOKEN, NONCE, "홍길동", null);

        appleLoginUsecase.executeAppLogin(req, ClientType.APP, request);

        OAuthUserInfoDTO passed = capturedUpsertInfo();
        assertThat(passed.nickname()).isEqualTo("홍길동");
        assertThat(passed.oauthId()).isEqualTo("apple-sub-001");
    }

    @Test
    @DisplayName("앱 로그인 — 2회차(name=null)에는 nickname이 null로 유지된다")
    void appLogin_withoutName_keepsNicknameNull() {
        given(identityTokenVerifier.verifyAndExtract(ID_TOKEN, ClientType.APP, NONCE)).willReturn(TOKEN_INFO);
        AppleAppLoginReqDTO req = new AppleAppLoginReqDTO(ID_TOKEN, NONCE, null, null);

        appleLoginUsecase.executeAppLogin(req, ClientType.APP, request);

        assertThat(capturedUpsertInfo().nickname()).isNull();
    }

    @Test
    @DisplayName("웹 콜백 — user 폼 필드의 이름이 lastName+firstName으로 조합되어 upsert에 전달된다")
    void webCallback_parsesUserPayloadName() {
        given(appleAuthService.exchangeCodeForToken("auth-code"))
                .willReturn(new AppleTokenResDTO("at", "bearer", 3600L, null, ID_TOKEN));
        given(identityTokenVerifier.verifyAndExtract(ID_TOKEN, ClientType.WEB, NONCE)).willReturn(TOKEN_INFO);
        String userPayload = "{\"name\":{\"firstName\":\"길동\",\"lastName\":\"홍\"},\"email\":\"x@y.com\"}";

        appleLoginUsecase.executeWebCallback("auth-code", NONCE, userPayload, request);

        assertThat(capturedUpsertInfo().nickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("웹 콜백 — user 파싱이 실패해도 로그인은 성공하고 nickname만 null이다")
    void webCallback_malformedUserPayload_doesNotFailLogin() {
        given(appleAuthService.exchangeCodeForToken("auth-code"))
                .willReturn(new AppleTokenResDTO("at", "bearer", 3600L, null, ID_TOKEN));
        given(identityTokenVerifier.verifyAndExtract(ID_TOKEN, ClientType.WEB, NONCE)).willReturn(TOKEN_INFO);

        appleLoginUsecase.executeWebCallback("auth-code", NONCE, "{broken-json", request);

        assertThat(capturedUpsertInfo().nickname()).isNull();
    }

    private OAuthUserInfoDTO capturedUpsertInfo() {
        ArgumentCaptor<OAuthUserInfoDTO> captor = ArgumentCaptor.forClass(OAuthUserInfoDTO.class);
        then(memberUpsertService).should().upsertRegisteringFromOAuth(eq(Provider.APPLE), captor.capture());
        return captor.getValue();
    }
}
