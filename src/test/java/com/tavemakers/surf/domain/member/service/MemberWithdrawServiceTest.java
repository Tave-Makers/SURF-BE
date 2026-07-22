package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent.SocialAccountSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawServiceTest {

    @Mock
    private MemberGetService memberGetService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberWithdrawService memberWithdrawService;

    private Member approvedMember(Long id, SocialAccount... socialAccounts) {
        Member member = Member.builder()
                .provider(Provider.KAKAO) // 레거시 컬럼(Phase 6 제거 예정) — 스냅샷 캡처와 무관
                .providerId("legacy-" + id)
                .name("홍길동")
                .email("hong" + id + "@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        for (SocialAccount socialAccount : socialAccounts) {
            member.addSocialAccount(socialAccount);
        }
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private SocialAccount kakaoAccount(Long kakaoId) {
        return SocialAccount.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(kakaoId))
                .kakaoId(kakaoId)
                .build();
    }

    private SocialAccount appleAccount(String appleRefreshToken) {
        return SocialAccount.builder()
                .provider(Provider.APPLE)
                .providerId("apple-sub-001")
                .appleRefreshToken(appleRefreshToken)
                .build();
    }

    private MemberDisconnectedEvent capturedEvent() {
        ArgumentCaptor<MemberDisconnectedEvent> captor = ArgumentCaptor.forClass(MemberDisconnectedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("withdraw — Kakao only 회원은 KAKAO 스냅샷 1건을 캡처한 뒤 탈퇴 상태로 전이하고 소셜 계정을 비운다")
    void withdraw_kakaoOnly_capturesSnapshotBeforeClear() {
        Member member = approvedMember(10L, kakaoAccount(123L));
        given(memberGetService.getMember(10L)).willReturn(member);

        memberWithdrawService.withdraw(10L);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.memberId()).isEqualTo(10L);
        assertThat(event.socialAccounts()).containsExactly(
                new SocialAccountSnapshot(Provider.KAKAO, 123L, null));

        // disconnectMember가 member.withdraw()보다 먼저 실행되어 clear() 전 값이 캡처된다.
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getSocialAccounts()).isEmpty();
    }

    @Test
    @DisplayName("withdraw — Apple only 회원은 APPLE 스냅샷에 revoke용 refresh token을 캡처한다")
    void withdraw_appleOnly_capturesRefreshToken() {
        Member member = approvedMember(11L, appleAccount("apple-refresh-token"));
        given(memberGetService.getMember(11L)).willReturn(member);

        memberWithdrawService.withdraw(11L);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.socialAccounts()).containsExactly(
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-refresh-token"));
        assertThat(member.getSocialAccounts()).isEmpty();
    }

    @Test
    @DisplayName("withdraw — Kakao+Apple 다중 연결 회원은 양쪽 provider 스냅샷을 모두 캡처한다")
    void withdraw_kakaoAndApple_capturesBothProviders() {
        Member member = approvedMember(12L, kakaoAccount(456L), appleAccount("apple-rt"));
        given(memberGetService.getMember(12L)).willReturn(member);

        memberWithdrawService.withdraw(12L);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.socialAccounts()).containsExactlyInAnyOrder(
                new SocialAccountSnapshot(Provider.KAKAO, 456L, null),
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt"));
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getSocialAccounts()).isEmpty();
    }

    @Test
    @DisplayName("withdraw — unlink/revoke 정보가 비어 있어도(누락) 스냅샷은 null 값 그대로 발행된다 (리스너가 생략 처리)")
    void withdraw_missingUnlinkInfo_publishesNullFields() {
        Member member = approvedMember(13L, appleAccount(null));
        given(memberGetService.getMember(13L)).willReturn(member);

        memberWithdrawService.withdraw(13L);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.socialAccounts()).containsExactly(
                new SocialAccountSnapshot(Provider.APPLE, null, null));
    }

    @Test
    @DisplayName("withdraw — 소셜 계정이 하나도 없으면 빈 스냅샷으로 발행되고 refresh 정리만 커밋 후 수행된다")
    void withdraw_noSocialAccounts_publishesEmptySnapshots() {
        Member member = approvedMember(14L);
        given(memberGetService.getMember(14L)).willReturn(member);

        memberWithdrawService.withdraw(14L);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.memberId()).isEqualTo(14L);
        assertThat(event.socialAccounts()).isEmpty();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("expel — 이미 WITHDRAWN 상태면 연결 해제 이벤트는 발행하되 withdraw()는 다시 수행하지 않는다")
    void expel_whenAlreadyWithdrawn_skipsWithdrawButStillDisconnects() {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("legacy-11")
                .name("홍길동")
                .email("hong2@test.com")
                .status(MemberStatus.WITHDRAWN)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        member.addSocialAccount(kakaoAccount(456L));
        ReflectionTestUtils.setField(member, "id", 11L);

        memberWithdrawService.expel(member);

        MemberDisconnectedEvent event = capturedEvent();
        assertThat(event.socialAccounts()).containsExactly(
                new SocialAccountSnapshot(Provider.KAKAO, 456L, null));
        // withdraw()가 호출되지 않았으므로 isDeleted/name/email 등 익명화 흔적이 없어야 한다.
        assertThat(member.isDeleted()).isFalse();
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getEmail()).isEqualTo("hong2@test.com");
        assertThat(member.getSocialAccounts()).hasSize(1);
    }
}
