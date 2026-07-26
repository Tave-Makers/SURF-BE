package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PendingSocialIntegrationTest {

    @Test
    @DisplayName("issue()는 예측 불가한 토큰과 now+TTL 만료를 설정한다")
    void issue_setsTokenAndExpiry() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", now);

        assertThat(pending.getToken()).isNotBlank();
        assertThat(pending.getToken()).doesNotContain("-");
        assertThat(pending.getTempMemberId()).isEqualTo(1L);
        assertThat(pending.getSocialAccountId()).isEqualTo(2L);
        assertThat(pending.getTargetMemberId()).isEqualTo(3L);
        assertThat(pending.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(pending.getNormalizedEmail()).isEqualTo("user@test.com");
        assertThat(pending.getNormalizedPhone()).isEqualTo("01011112222");
        assertThat(pending.getExpiresAt()).isEqualTo(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS));
    }

    @Test
    @DisplayName("isExpired()는 expiresAt 이상이면 true")
    void isExpired_afterExpiry() {
        LocalDateTime now = LocalDateTime.now();
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", now);

        assertThat(pending.isExpired(now)).isFalse();
        assertThat(pending.isExpired(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS - 1))).isFalse();
        // 경계: 정확히 expiresAt(= now + TTL)이면 만료로 판정한다
        assertThat(pending.isExpired(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS))).isTrue();
        assertThat(pending.isExpired(now.plusSeconds(PendingSocialIntegration.TTL_SECONDS + 1))).isTrue();
    }

    @Test
    @DisplayName("matchesContext()는 임시 회원·대상·provider·연락처가 모두 같을 때만 true")
    void matchesContext_allFields() {
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", LocalDateTime.now());

        assertThat(pending.matchesContext(1L, 3L, Provider.KAKAO, "user@test.com", "01011112222")).isTrue();
        // 대상이 바뀐 재요청은 다른 명령이므로 멱등 재사용 대상이 아니다 — 빠뜨리면 이전 대상 토큰이 재사용된다
        assertThat(pending.matchesContext(1L, 999L, Provider.KAKAO, "user@test.com", "01011112222")).isFalse();
        assertThat(pending.matchesContext(999L, 3L, Provider.KAKAO, "user@test.com", "01011112222")).isFalse();
        assertThat(pending.matchesContext(1L, 3L, Provider.APPLE, "user@test.com", "01011112222")).isFalse();
        assertThat(pending.matchesContext(1L, 3L, Provider.KAKAO, "other@test.com", "01011112222")).isFalse();
        assertThat(pending.matchesContext(1L, 3L, Provider.KAKAO, "user@test.com", "01099998888")).isFalse();
    }

    @Test
    @DisplayName("isTargetMember()는 감지 시점에 확정된 대상만 true")
    void isTargetMember() {
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", LocalDateTime.now());

        assertThat(pending.isTargetMember(3L)).isTrue();
        assertThat(pending.isTargetMember(4L)).isFalse();
    }

    @Test
    @DisplayName("targetMemberId가 비어 있는 구버전 행 — NPE 없이 재사용 거부·통합 거부로 처리한다")
    void legacyRow_withoutTarget() {
        // 컬럼 추가(expand) 직후 구버전 인스턴스가 만든 행: target_member_id = NULL
        PendingSocialIntegration legacy = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", LocalDateTime.now());
        ReflectionTestUtils.setField(legacy, "targetMemberId", null);

        assertThat(legacy.hasTargetMember()).isFalse();
        // 동일 컨텍스트로 보지 않는다 → 삭제 후 재발급 경로로 흘러간다
        assertThat(legacy.matchesContext(1L, 3L, Provider.KAKAO, "user@test.com", "01011112222")).isFalse();
        // 어떤 주체와도 일치하지 않는다 → 통합 거부
        assertThat(legacy.isTargetMember(3L)).isFalse();
        assertThat(legacy.isTargetMember(null)).isFalse();
    }

    @Test
    @DisplayName("matchesContactInfo()는 이메일·전화번호가 모두 같을 때만 true")
    void matchesContactInfo() {
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                1L, 2L, 3L, Provider.KAKAO, "user@test.com", "01011112222", LocalDateTime.now());

        assertThat(pending.matchesContactInfo("user@test.com", "01011112222")).isTrue();
        assertThat(pending.matchesContactInfo("other@test.com", "01011112222")).isFalse();
        assertThat(pending.matchesContactInfo("user@test.com", "01099998888")).isFalse();
        assertThat(pending.matchesContactInfo(null, null)).isFalse();
    }
}
