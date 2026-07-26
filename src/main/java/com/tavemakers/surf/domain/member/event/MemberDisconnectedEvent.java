package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.SocialAccount;

import java.util.List;

/**
 * 회원 연결 해제 이벤트 — 탈퇴/퇴출/제명 시 발행.
 * 외부 연결 해제(Kakao unlink / Apple revoke)와 refresh 토큰 무효화를
 * 트랜잭션 커밋 이후로 미루기 위한 이벤트다.
 *
 * <p>주의: member.withdraw()가 socialAccounts.clear()로 소셜 계정을 지우므로,
 * 발행 시점에 연결된 모든 소셜 계정의 provider별 값을 불변 스냅샷으로 캡처해 전달한다
 * (JPA 엔티티를 담으면 AFTER_COMMIT 리스너 실행 시점에 이미 값이 사라짐).
 */
public record MemberDisconnectedEvent(
        Long memberId,
        List<SocialAccountSnapshot> socialAccounts
) {

    public MemberDisconnectedEvent {
        socialAccounts = List.copyOf(socialAccounts);
    }

    /** 연결된 모든 소셜 계정의 스냅샷으로 이벤트를 생성한다 — withdraw()의 clear() 이전에 호출해야 한다. */
    public static MemberDisconnectedEvent from(Long memberId, List<SocialAccount> accounts) {
        return new MemberDisconnectedEvent(
                memberId,
                accounts.stream().map(SocialAccountSnapshot::from).toList()
        );
    }

    /** provider별 unlink/revoke 정보 불변 스냅샷 — 리스너는 이 값만으로 외부 해제를 수행한다. */
    public record SocialAccountSnapshot(
            Provider provider,
            Long kakaoId,
            String appleRefreshToken
    ) {
        public static SocialAccountSnapshot from(SocialAccount socialAccount) {
            return new SocialAccountSnapshot(
                    socialAccount.getProvider(),
                    socialAccount.getKakaoId(),
                    socialAccount.getAppleRefreshToken()
            );
        }
    }
}
