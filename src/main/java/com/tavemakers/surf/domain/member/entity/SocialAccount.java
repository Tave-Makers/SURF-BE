package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.presentation.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.exception.InvalidMemberInfoException;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * provider별 로그인 수단 단위 엔티티 — {@code Member} 와 N : 1 관계.
 * 한 회원이 Kakao/Apple 등 여러 소셜 계정을 연결할 수 있으며, 어느 쪽으로 로그인해도 같은 회원으로 인증된다.
 */
@Entity
@Table(
        name = "social_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        },
        indexes = {
                @Index(name = "idx_social_member_id", columnList = "member_id"),
                @Index(name = "idx_social_provider", columnList = "provider")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    /**
     * 소셜 로그인이 전달한 이메일. 회원 식별에는 사용하지 않는다.
     * 로그인 시점 provider 값으로만 채우며, 레거시 이관 행은 null 로 남을 수 있다(카카오 원본 이메일 백필하지 않음).
     */
    @Column(name = "provider_email")
    private String providerEmail;

    /** Apple 계정 탈퇴(revoke) 시 사용. 로그인 코드 교환 후 저장, 탈퇴 시 null 처리. */
    @Column(name = "apple_refresh_token", length = 1024)
    private String appleRefreshToken;

    /** 카카오 unlink용 레거시 식별자. */
    @Column(name = "kakao_id")
    private Long kakaoId;

    @Builder
    private SocialAccount(Provider provider,
                          String providerId,
                          String providerEmail,
                          String appleRefreshToken,
                          Long kakaoId) {
        this.provider = provider;
        this.providerId = providerId;
        this.providerEmail = providerEmail;
        this.appleRefreshToken = appleRefreshToken;
        this.kakaoId = kakaoId;
    }

    /**
     * OAuth provider 정보로 SocialAccount 를 생성한다.
     * providerEmail 은 소셜이 준 이메일을 그대로 저장하며, 회원 식별에는 사용하지 않는다.
     */
    public static SocialAccount createFromOAuth(Provider provider, OAuthUserInfoDTO info) {
        if (provider == null) {
            throw new IllegalStateException("provider 는 필수입니다.");
        }
        if (info.oauthId() == null || info.oauthId().isBlank()) {
            throw new IllegalStateException(provider + " 식별자(oauthId)가 비어 있습니다.");
        }

        Long legacyKakaoId = null;
        if (provider == Provider.KAKAO) {
            try {
                legacyKakaoId = Long.parseLong(info.oauthId());
            } catch (NumberFormatException e) {
                throw new InvalidMemberInfoException(
                        "Provider.KAKAO oauthId가 유효한 숫자 형식이 아닙니다: " + info.oauthId());
            }
        }

        return SocialAccount.builder()
                .provider(provider)
                .providerId(info.oauthId())
                .providerEmail(info.email())
                .kakaoId(legacyKakaoId)
                .build();
    }

    /**
     * 소속 회원을 설정하고 양방향 연관관계 일관성을 유지한다.
     * 통합(연동) 시 기존 회원으로 재연결하는 용도로도 사용한다.
     */
    public void setMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("member는 null일 수 없습니다.");
        }

        if (this.member == member) return;

        // 기존 관계 끊기
        if (this.member != null) {
            this.member.getSocialAccounts().remove(this);
        }

        this.member = member;

        // 양방향 일관성 유지
        if (!member.getSocialAccounts().contains(this)) {
            member.getSocialAccounts().add(this);
        }
    }

    /** 로그인 시점에 provider 가 전달한 이메일로 갱신한다(레거시 백필 대체). */
    public void updateProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    /** Apple refresh_token 갱신 — 로그인 코드 교환 시 호출. */
    public void updateAppleRefreshToken(String refreshToken) {
        this.appleRefreshToken = refreshToken;
    }
}
