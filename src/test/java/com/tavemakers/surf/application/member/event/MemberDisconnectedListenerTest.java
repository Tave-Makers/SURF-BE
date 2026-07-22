package com.tavemakers.surf.application.member.event;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent.SocialAccountSnapshot;
import com.tavemakers.surf.infrastructure.auth.apple.AppleApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberDisconnectedListenerTest {

    private static final String UNLINK_URI = "https://kapi.kakao.test/v1/user/unlink";

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AppleApiClient appleApiClient;

    @Mock
    private RestTemplate restTemplate;

    private MemberDisconnectedListener listener;

    @BeforeEach
    void setUp() {
        listener = new MemberDisconnectedListener(refreshTokenService, appleApiClient, restTemplate);
        ReflectionTestUtils.setField(listener, "adminKey", "test-admin-key");
        ReflectionTestUtils.setField(listener, "unlinkUri", UNLINK_URI);
    }

    private MemberDisconnectedEvent event(SocialAccountSnapshot... snapshots) {
        return new MemberDisconnectedEvent(1L, List.of(snapshots));
    }

    @Test
    @DisplayName("Kakao+Apple 다중 연결 회원은 refresh 무효화와 함께 양쪽 provider 모두 해제한다")
    void handle_kakaoAndApple_disconnectsBoth() {
        listener.handle(event(
                new SocialAccountSnapshot(Provider.KAKAO, 123L, null),
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt")));

        then(refreshTokenService).should().invalidateAll(1L);
        then(restTemplate).should().postForEntity(eq(UNLINK_URI), any(HttpEntity.class), eq(String.class));
        then(appleApiClient).should().revokeAppToken("apple-rt");
        then(appleApiClient).should().revokeToken("apple-rt");
    }

    @Test
    @DisplayName("refresh 토큰 무효화가 실패해도 provider 연결 해제는 계속 수행된다 (장애 격리)")
    void handle_refreshInvalidationFailure_doesNotBlockProviders() {
        willThrow(new IllegalStateException("redis down"))
                .given(refreshTokenService).invalidateAll(anyLong());

        assertThatCode(() -> listener.handle(event(
                new SocialAccountSnapshot(Provider.KAKAO, 123L, null),
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt"))))
                .doesNotThrowAnyException();

        then(restTemplate).should().postForEntity(eq(UNLINK_URI), any(HttpEntity.class), eq(String.class));
        then(appleApiClient).should().revokeAppToken("apple-rt");
    }

    @Test
    @DisplayName("먼저 처리한 Apple revoke가 실패해도 Kakao unlink는 계속 수행된다 (provider 간 장애 격리)")
    void handle_appleFailure_doesNotBlockKakao() {
        willThrow(new IllegalStateException("apple down"))
                .given(appleApiClient).revokeAppToken(anyString());

        assertThatCode(() -> listener.handle(event(
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt"),
                new SocialAccountSnapshot(Provider.KAKAO, 123L, null))))
                .doesNotThrowAnyException();

        then(restTemplate).should().postForEntity(eq(UNLINK_URI), any(HttpEntity.class), eq(String.class));
        then(refreshTokenService).should().invalidateAll(1L);
    }

    @Test
    @DisplayName("먼저 처리한 Kakao unlink가 예기치 못한 오류로 실패해도 Apple revoke는 계속 수행된다")
    void handle_kakaoFailure_doesNotBlockApple() {
        willThrow(new IllegalStateException("kakao down"))
                .given(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

        assertThatCode(() -> listener.handle(event(
                new SocialAccountSnapshot(Provider.KAKAO, 123L, null),
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt"))))
                .doesNotThrowAnyException();

        then(appleApiClient).should().revokeAppToken("apple-rt");
        then(appleApiClient).should().revokeToken("apple-rt");
    }

    @Test
    @DisplayName("누락 정보 — kakaoId가 없으면 unlink를, appleRefreshToken이 없으면 revoke를 생략한다")
    void handle_missingUnlinkInfo_skipsExternalCalls() {
        listener.handle(event(
                new SocialAccountSnapshot(Provider.KAKAO, null, null),
                new SocialAccountSnapshot(Provider.APPLE, null, null)));

        then(refreshTokenService).should().invalidateAll(1L);
        then(restTemplate).should(never()).postForEntity(anyString(), any(), any());
        then(appleApiClient).should(never()).revokeAppToken(anyString());
        then(appleApiClient).should(never()).revokeToken(anyString());
    }

    @Test
    @DisplayName("소셜 계정 스냅샷이 비어 있으면 refresh 무효화만 수행한다")
    void handle_emptySnapshots_onlyInvalidatesRefreshTokens() {
        listener.handle(event());

        then(refreshTokenService).should().invalidateAll(1L);
        then(restTemplate).should(never()).postForEntity(anyString(), any(), any());
        then(appleApiClient).should(never()).revokeAppToken(anyString());
    }
}
