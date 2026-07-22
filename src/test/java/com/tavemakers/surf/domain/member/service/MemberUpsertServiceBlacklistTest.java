package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.MemberBlacklist;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.repository.MemberBlacklistRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로그인 upsert 블랙리스트 기준 회귀 테스트 (5.A-8) —
 * 기준은 통합 이메일/전화번호(Member.email / Member.phoneNumber)이며,
 * provider가 준 이메일(SocialAccount.providerEmail)은 차단 기준이 아니다.
 */
@DataJpaTest
@Import({MemberUpsertService.class, MemberBlacklistGetService.class})
class MemberUpsertServiceBlacklistTest {

    @Autowired
    private MemberUpsertService memberUpsertService;

    @Autowired
    private MemberBlacklistRepository memberBlacklistRepository;

    @Autowired
    private EntityManager em;

    private static final String BLOCKED_EMAIL = "blocked@test.com";
    private static final String BLOCKED_PHONE = "01012345678";

    /** 온보딩 완료(APPROVED) 회원 — 통합 이메일/전화번호 보유. */
    private Member approvedMember(String email, String phone) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("legacy-" + System.nanoTime())
                .name("기존회원")
                .email(email)
                .phoneNumber(phone)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        em.persist(member);
        return member;
    }

    private void blacklist(Member member, String email, String phone) {
        memberBlacklistRepository.save(
                MemberBlacklist.of(member, MemberBlacklistActionType.EXPEL, 999L, email, phone));
        em.flush();
    }

    @Test
    @DisplayName("신규 OAuth 사용자는 provider 이메일이 블랙리스트와 같아도 차단되지 않고 REGISTERING으로 가입된다")
    void newOAuthUser_isNotBlockedByProviderEmail() {
        Member expelled = approvedMember(BLOCKED_EMAIL, BLOCKED_PHONE);
        blacklist(expelled, BLOCKED_EMAIL, BLOCKED_PHONE);

        // provider가 준 이메일이 차단된 이메일과 동일해도 로그인 단계에서는 차단하지 않는다
        // (재가입 차단은 온보딩/계정 통합의 통합 이메일·전화번호 검증이 담당)
        OAuthUserInfoDTO info = new OAuthUserInfoDTO("111222333", BLOCKED_EMAIL, "신규", null);

        Member created = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, info);
        em.flush();

        assertThat(created.getId()).isNotEqualTo(expelled.getId());
        assertThat(created.getStatus()).isEqualTo(MemberStatus.REGISTERING);
        assertThat(created.getEmail()).as("통합 이메일은 온보딩 전이라 비어 있다").isNull();
    }

    @Test
    @DisplayName("기존 회원 로그인은 통합 이메일이 블랙리스트에 있으면 차단된다")
    void existingMember_isBlockedByUnifiedEmail() {
        OAuthUserInfoDTO info = new OAuthUserInfoDTO("444555666", "kakao-side@kakao.com", "회원", null);
        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, info);
        member.applySignup("회원", "대학", null, BLOCKED_EMAIL, BLOCKED_PHONE);
        em.flush();

        Member other = approvedMember("other@test.com", "01099998888");
        blacklist(other, BLOCKED_EMAIL, null);

        assertThatThrownBy(() -> memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, info))
                .isInstanceOf(MemberBlacklistedException.class);
    }

    @Test
    @DisplayName("기존 회원 로그인은 전화번호가 블랙리스트에 있으면 차단된다")
    void existingMember_isBlockedByPhoneNumber() {
        OAuthUserInfoDTO info = new OAuthUserInfoDTO("777888999", null, "회원", null);
        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, info);
        member.applySignup("회원", "대학", null, "member@test.com", BLOCKED_PHONE);
        em.flush();

        Member other = approvedMember("other2@test.com", "01077776666");
        blacklist(other, "unrelated@test.com", BLOCKED_PHONE);

        assertThatThrownBy(() -> memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, info))
                .isInstanceOf(MemberBlacklistedException.class);
    }

    @Test
    @DisplayName("기존 회원 로그인은 블랙리스트와 무관하면 정상 진입한다")
    void existingMember_notBlacklisted_logsInNormally() {
        OAuthUserInfoDTO info = new OAuthUserInfoDTO("121212121", "clean@kakao.com", "회원", null);
        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, info);
        member.applySignup("회원", "대학", null, "clean@test.com", "01055554444");
        em.flush();

        Member other = approvedMember("expelled@test.com", "01011112222");
        blacklist(other, "expelled@test.com", "01011112222");

        Member loggedIn = memberUpsertService.upsertRegisteringFromOAuth(Provider.KAKAO, info);
        assertThat(loggedIn.getId()).isEqualTo(member.getId());
    }
}
