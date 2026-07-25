package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계정 통합 대기 row — 온보딩 case B(통합 필요 감지) 시 발급되는 1회성 연결 작업 식별자.
 * MemberStatus를 늘리지 않고 별도 row로 통합 대기를 관리하며, 통합 성공 시 즉시 삭제(hard delete)한다. (§3.6.1, 5.A-12)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "pending_social_integration",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pending_integration_token", columnNames = "token"),
                @UniqueConstraint(name = "uk_pending_integration_social_account", columnNames = "social_account_id")
        }
)
public class PendingSocialIntegration extends BaseEntity {

    /** 통합 대기 TTL — 30분 (5.A-10) */
    public static final long TTL_SECONDS = 1800L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_social_integration_id")
    private Long id;

    /** 프론트에 전달할 1회성 통합 토큰 (예측 불가·UNIQUE) */
    @Column(nullable = false, length = 64)
    private String token;

    /** 신규 소셜 로그인으로 생성된 임시 REGISTERING 회원 */
    @Column(nullable = false)
    private Long tempMemberId;

    /** 기존 회원으로 옮길 SocialAccount — 계정당 pending 1개(UNIQUE)로 유효 토큰을 상한한다 */
    @Column(nullable = false)
    private Long socialAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider;

    /** 통합 필요 감지 시 온보딩 입력 이메일 — 통합 API 최종 재검증 근거 */
    @Column(nullable = false)
    private String normalizedEmail;

    /** 통합 필요 감지 시 온보딩 입력 전화번호 — 통합 API 최종 재검증 근거 */
    @Column(nullable = false)
    private String normalizedPhone;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    private PendingSocialIntegration(String token, Long tempMemberId, Long socialAccountId,
                                     Provider provider, String normalizedEmail, String normalizedPhone,
                                     LocalDateTime expiresAt) {
        this.token = token;
        this.tempMemberId = tempMemberId;
        this.socialAccountId = socialAccountId;
        this.provider = provider;
        this.normalizedEmail = normalizedEmail;
        this.normalizedPhone = normalizedPhone;
        this.expiresAt = expiresAt;
    }

    /** 임시 회원·소셜 계정·온보딩 입력값으로 대기 row를 발급한다. 토큰은 1회성 예측 불가 값, 만료는 {@code now + TTL}. */
    public static PendingSocialIntegration issue(Long tempMemberId, Long socialAccountId, Provider provider,
                                                 String normalizedEmail, String normalizedPhone, LocalDateTime now) {
        return PendingSocialIntegration.builder()
                .token(UUID.randomUUID().toString().replace("-", ""))
                .tempMemberId(tempMemberId)
                .socialAccountId(socialAccountId)
                .provider(provider)
                .normalizedEmail(normalizedEmail)
                .normalizedPhone(normalizedPhone)
                .expiresAt(now.plusSeconds(TTL_SECONDS))
                .build();
    }

    /** 만료 여부 — {@code expiresAt} 이상이면 true(경계 시각에 TTL 소진). */
    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    /** 발급 컨텍스트 동일성 — 임시 회원·provider·연락처가 같은 재요청만 멱등 재사용 대상이다(연락처가 바뀐 요청은 다른 명령). */
    public boolean matchesContext(Long tempMemberId, Provider provider,
                                  String normalizedEmail, String normalizedPhone) {
        return this.tempMemberId.equals(tempMemberId)
                && this.provider == provider
                && this.normalizedEmail.equals(normalizedEmail)
                && this.normalizedPhone.equals(normalizedPhone);
    }
}
