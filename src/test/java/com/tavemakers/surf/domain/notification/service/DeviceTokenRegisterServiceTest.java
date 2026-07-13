package com.tavemakers.surf.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.entity.Platform;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * DeviceTokenRegisterService.register 멱등 등록 로직 단위 테스트.
 * 토큰이 이미 존재하면(같은 소유자든 다른 소유자든) 신규 저장 없이 기존 행을 갱신해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenRegisterServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private DeviceTokenRegisterService deviceTokenRegisterService;

    private final String token = "fcm-token";

    @Test
    @DisplayName("토큰이 처음 등록되는 경우 새 디바이스 토큰을 저장한다")
    void register_신규_토큰이면_저장한다() {
        given(deviceTokenRepository.findByToken(token)).willReturn(Optional.empty());

        deviceTokenRegisterService.register(1L, token, Platform.ANDROID);

        then(deviceTokenRepository).should().save(
                argThat(saved ->
                        saved.getMemberId().equals(1L)
                                && saved.getToken().equals(token)
                                && saved.getPlatform() == Platform.ANDROID)
        );
    }

    @Test
    @DisplayName("같은 회원이 재등록하면 저장하지 않고(멱등) 마지막 사용 시각과 활성 상태만 갱신한다")
    void register_같은_소유자가_재등록하면_저장없이_touch만_수행한다() {
        DeviceToken existing = DeviceToken.builder()
                .memberId(1L).token(token).platform(Platform.ANDROID).build();
        LocalDateTime staleSeenAt = LocalDateTime.now().minusDays(30);
        ReflectionTestUtils.setField(existing, "lastSeenAt", staleSeenAt);
        ReflectionTestUtils.setField(existing, "enabled", false);
        given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existing));

        deviceTokenRegisterService.register(1L, token, Platform.ANDROID);

        assertThat(existing.getMemberId()).isEqualTo(1L);
        assertThat(existing.isEnabled()).isTrue();
        assertThat(existing.getLastSeenAt()).isAfter(staleSeenAt);
        then(deviceTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("다른 회원이 동일 토큰으로 등록하면(기기 재사용) 저장 없이 소유자를 교체한다")
    void register_다른_소유자가_등록하면_저장없이_소유자를_교체한다() {
        DeviceToken existing = DeviceToken.builder()
                .memberId(1L).token(token).platform(Platform.IOS).build();
        given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existing));

        deviceTokenRegisterService.register(2L, token, Platform.IOS);

        assertThat(existing.getMemberId()).isEqualTo(2L);
        assertThat(existing.isEnabled()).isTrue();
        then(deviceTokenRepository).should(never()).save(any());
    }
}
