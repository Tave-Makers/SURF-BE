package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탈퇴 회원의 소셜 재로그인(재가입) 회귀 테스트.
 *
 * <p>버그: 소셜 로그인 통합(Phase 2) 이후 로그인 식별이 social_account 기준인데,
 * 탈퇴(withdraw)가 Member 컬럼만 익명화하고 social_account 행을 남겼다.
 * 재로그인 시 잔존 행이 WITHDRAWN 회원에 매칭돼 신규 가입 경로를 타지 못하고,
 * 발급된 토큰은 필터에서 401 — 같은 소셜 계정으로 영구히 재가입 불가였다.
 *
 * <p>수정: (1) withdraw 가 socialAccounts 를 함께 제거(orphanRemoval),
 * (2) upsert 가 WITHDRAWN 매칭 시 잔존 행을 정리하고 신규 가입으로 보냄(기존 오염 데이터 자가 치유).
 */
@DataJpaTest
@Import({MemberUpsertService.class, MemberBlacklistGetService.class})
class WithdrawnMemberRejoinTest {

    private static final String KAKAO_ID = "9990001112223334";

    @Autowired
    private MemberUpsertService memberUpsertService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private EntityManager em;

    private OAuthUserInfoDTO kakaoInfo() {
        return new OAuthUserInfoDTO(KAKAO_ID, "user@kakao.com", "서퍼", null);
    }

    /** 첫 로그인으로 REGISTERING 회원 + SocialAccount 생성 */
    private Member firstLogin() {
        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, kakaoInfo());
        em.flush();
        return member;
    }

    @Test
    @DisplayName("withdraw 하면 social_account 도 함께 삭제된다 (잔존 → 재가입 불가의 근원 차단)")
    void 탈퇴하면_소셜계정도_삭제된다() {
        Member member = firstLogin();
        assertThat(socialAccountRepository.findByProviderAndProviderId(Provider.KAKAO, KAKAO_ID)).isPresent();

        member.withdraw();
        em.flush();

        assertThat(socialAccountRepository.findByProviderAndProviderId(Provider.KAKAO, KAKAO_ID))
                .as("탈퇴 시 소셜 계정 연결이 DB에서 제거되어야 한다")
                .isEmpty();
    }

    @Test
    @DisplayName("탈퇴 후 같은 카카오 계정으로 재로그인하면 새 REGISTERING 회원으로 가입된다")
    void 탈퇴_후_재로그인은_신규가입이다() {
        Member old = firstLogin();
        Long oldId = old.getId();
        old.withdraw();
        em.flush();

        Member rejoined = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, kakaoInfo());
        em.flush();

        assertThat(rejoined.getId()).as("탈퇴한 옛 계정이 아니라 새 회원이어야 한다").isNotEqualTo(oldId);
        assertThat(rejoined.getStatus()).isEqualTo(MemberStatus.REGISTERING);
    }

    @Test
    @DisplayName("자가 치유: 잔존 social_account(수정 전 탈퇴 데이터)가 있어도 재로그인이 신규 가입으로 진행된다")
    void 기존_오염_데이터도_재로그인시_자가치유된다() {
        // 수정 전 withdraw 상태를 재현: member 는 익명화·WITHDRAWN 완료됐지만 social_account 만 잔존.
        // (실운영 오염 데이터와 동일 — 옛 withdraw 는 member 컬럼은 익명화했고 social_account 는 안 지웠다)
        Member old = firstLogin();
        Long oldId = old.getId();
        old.withdraw();
        em.flush();
        SocialAccount leftover = SocialAccount.createFromOAuth(Provider.KAKAO, kakaoInfo());
        old.addSocialAccount(leftover);
        em.flush();
        assertThat(socialAccountRepository.findByProviderAndProviderId(Provider.KAKAO, KAKAO_ID))
                .as("전제: 오염 데이터(잔존 행) 존재").isPresent();

        Member rejoined = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, kakaoInfo());
        em.flush();

        assertThat(rejoined.getId()).isNotEqualTo(oldId);
        assertThat(rejoined.getStatus()).isEqualTo(MemberStatus.REGISTERING);
        // 잔존 행은 정리되고, 새 회원의 소셜 계정이 그 자리를 대체
        assertThat(socialAccountRepository.findByProviderAndProviderId(Provider.KAKAO, KAKAO_ID))
                .isPresent()
                .get()
                .extracting(SocialAccount::getMember)
                .extracting(Member::getId)
                .isEqualTo(rejoined.getId());
    }
}
