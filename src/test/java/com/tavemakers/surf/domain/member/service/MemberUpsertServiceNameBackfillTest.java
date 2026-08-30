package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그인 upsert의 이름 저장/복구 회귀 테스트 (이슈 #392) —
 * Apple은 이름을 최초 인가 1회만 주므로, 신규 가입 시 저장하고
 * 과거 유실 회원은 인가 취소 후 재로그인 시 백필한다. 저장된 이름은 덮어쓰지 않는다.
 */
@DataJpaTest
@Import({MemberUpsertService.class, MemberBlacklistGetService.class})
class MemberUpsertServiceNameBackfillTest {

    private static final String APPLE_SUB = "apple-sub-392";

    @Autowired
    private MemberUpsertService memberUpsertService;

    @Autowired
    private EntityManager em;

    private Member existingAppleMember(String name) {
        OAuthUserInfoDTO original = new OAuthUserInfoDTO(APPLE_SUB, "relay@privaterelay.appleid.com", name, null);
        Member member = Member.createRegisteringFromOAuth(original);
        member.addSocialAccount(SocialAccount.createFromOAuth(Provider.APPLE, original));
        em.persist(member);
        em.flush();
        return member;
    }

    @Test
    @DisplayName("신규 Apple 가입 시 user로 받은 이름이 member.name에 저장된다")
    void newMember_savesName() {
        OAuthUserInfoDTO info = new OAuthUserInfoDTO(APPLE_SUB, "relay@privaterelay.appleid.com", "홍길동", null);

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, info);

        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.REGISTERING);
    }

    @Test
    @DisplayName("이름이 유실된 기존 회원은 재인가 로그인에서 이름이 백필된다")
    void existingMemberWithoutName_backfillsName() {
        existingAppleMember(null);
        OAuthUserInfoDTO relogin = new OAuthUserInfoDTO(APPLE_SUB, "relay@privaterelay.appleid.com", "홍길동", null);

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, relogin);

        assertThat(member.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("이미 이름이 있는 기존 회원은 provider가 다른 이름을 줘도 덮어쓰지 않는다")
    void existingMemberWithName_isNotOverwritten() {
        existingAppleMember("기존이름");
        OAuthUserInfoDTO relogin = new OAuthUserInfoDTO(APPLE_SUB, "relay@privaterelay.appleid.com", "새이름", null);

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, relogin);

        assertThat(member.getName()).isEqualTo("기존이름");
    }

    @Test
    @DisplayName("2회차 로그인(name=null)은 저장된 이름을 건드리지 않는다")
    void reloginWithoutName_keepsStoredName() {
        existingAppleMember("홍길동");
        OAuthUserInfoDTO relogin = new OAuthUserInfoDTO(APPLE_SUB, "relay@privaterelay.appleid.com", null, null);

        Member member = memberUpsertService.upsertRegisteringFromOAuth(Provider.APPLE, relogin);

        assertThat(member.getName()).isEqualTo("홍길동");
    }
}
