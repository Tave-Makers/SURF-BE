package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberUpsertService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberBlacklistGetService memberBlacklistGetService;

    /**
     * 소셜 로그인 upsert — SocialAccount 기준으로 조회하고, 없으면 REGISTERING 회원 + SocialAccount 를 생성한다.
     * <p>로그인 단계에서는 email/phone 기반 자동 통합을 하지 않는다(명시적 연동만 허용, EmailConflictException 폐기).
     * 반환 엔티티는 현재 트랜잭션에서 managed 상태이므로 호출자의 후속 변경(refresh_token 등)이 커밋에 함께 반영된다.
     * <p>트랜잭션 경계는 호출자(AppleLoginUsecase/KakaoLoginUsecase)가 소유한다.
     */
    public Member upsertRegisteringFromOAuth(Provider provider, OAuthUserInfoDTO info) {
        return findExistingMember(provider, info)
                .orElseGet(() -> createNewSocialMember(provider, info));
    }

    /**
     * SocialAccount 로 기존 회원을 조회하고, 있으면 provider 가 준 이메일로 providerEmail 을 갱신한다.
     * <p>탈퇴(WITHDRAWN) 회원에 매칭되면 잔존 SocialAccount 를 정리하고 빈 결과를 반환해
     * 신규 가입 경로로 보낸다 — withdraw 가 소셜 계정을 지우기 전에 탈퇴한 기존 데이터의 자가 치유.
     * (탈퇴자가 재로그인하면 새 계정으로 가입 시작. 퇴출/제명자는 블랙리스트가 신규 가입을 차단한다.)
     */
    private Optional<Member> findExistingMember(Provider provider, OAuthUserInfoDTO info) {
        return socialAccountRepository.findByProviderAndProviderId(provider, info.oauthId())
                .flatMap(socialAccount -> {
                    Member member = socialAccount.getMember();
                    if (member.getStatus() == MemberStatus.WITHDRAWN) {
                        // 부모 컬렉션에서 제거(orphanRemoval 삭제). 이어지는 신규 가입 INSERT가
                        // 같은 (provider, provider_id) UNIQUE 를 쓰므로 DELETE 를 먼저 flush 한다.
                        member.removeSocialAccount(socialAccount);
                        socialAccountRepository.flush();
                        return Optional.empty();
                    }
                    refreshProviderEmail(socialAccount, info.email());
                    return Optional.of(member);
                });
    }

    /**
     * 신규 소셜 계정 로그인 — REGISTERING 회원 + SocialAccount 를 현재 트랜잭션에서 생성/연결한다.
     * <p>동시 첫 로그인 경합 시 한쪽은 UNIQUE 제약 위반으로 트랜잭션이 롤백되며(재시도 시 정상 진입), 오염된 세션에서 복구를 시도하지 않는다.
     */
    private Member createNewSocialMember(Provider provider, OAuthUserInfoDTO info) {
        Long kakaoId = provider == Provider.KAKAO ? Long.parseLong(info.oauthId()) : null;
        memberBlacklistGetService.validateNotBlacklisted(kakaoId, info.email(), null);

        Member member = Member.createRegisteringFromOAuth(provider, info);
        SocialAccount socialAccount = SocialAccount.createFromOAuth(provider, info);
        member.addSocialAccount(socialAccount);

        return memberRepository.save(member);
    }

    /**
     * 재로그인 시 provider 가 준 이메일로 providerEmail 을 갱신한다(레거시 이관 행의 null 백필 대체, 설계 3.6).
     * <p>Apple 은 최초 로그인 이후 이메일을 주지 않으므로 값이 있을 때만 갱신해 기존 값을 지우지 않는다.
     */
    private void refreshProviderEmail(SocialAccount socialAccount, String providerEmail) {
        if (providerEmail == null || providerEmail.isBlank()) {
            return;
        }
        if (!providerEmail.equals(socialAccount.getProviderEmail())) {
            socialAccount.updateProviderEmail(providerEmail);
        }
    }
}
