package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PendingSocialIntegrationTest {

    @Test
    @DisplayName("issue()는 예측 불가한 토큰과 now+TTL 만료를 설정한다")
    void issue_setsTokenAndExpiry() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, Provider.KAKAO, "user@test.com", "01011112222", now);

        assertThat(pending.getToken()).isNotBlank();
        assertThat(pending.getToken()).doesNotContain("-");
        assertThat(pending.getTempMemberId()).isEqualTo(1L);
        assertThat(pending.getSocialAccountId()).isEqualTo(2L);
        assertThat(pending.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(pending.getNormalizedEmail()).isEqualTo("user@test.com");
        assertThat(pending.getNormalizedPhone()).isEqualTo("01011112222");
        assertThat(pending.getExpiresAt()).isEqualTo(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS));
    }

    @Test
    @DisplayName("isExpired()는 expiresAt 경과 시에만 true")
    void isExpired_afterExpiry() {
        LocalDateTime now = LocalDateTime.now();
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, Provider.KAKAO, "user@test.com", "01011112222", now);

        assertThat(pending.isExpired(now)).isFalse();
        assertThat(pending.isExpired(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS - 1))).isFalse();
        assertThat(pending.isExpired(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS + 1))).isTrue();
    }
}
