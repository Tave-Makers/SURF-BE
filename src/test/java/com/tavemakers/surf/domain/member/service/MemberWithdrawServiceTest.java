package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;
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
import static org.mockito.ArgumentMatchers.any;
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

    private Member approvedMember(Long id) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao-1")
                .kakaoId(123L)
                .name("홍길동")
                .email("hong@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        member.updateAppleRefreshToken("apple-refresh-token");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("withdraw는 회원을 조회한 뒤, 익명화 전 원본 정보로 연결 해제 이벤트를 발행하고 탈퇴 상태로 전이한다")
    void withdraw_publishesEventWithOriginalValuesBeforeAnonymizingThenWithdraws() {
        Member member = approvedMember(10L);
        given(memberGetService.getMember(10L)).willReturn(member);

        memberWithdrawService.withdraw(10L);

        ArgumentCaptor<MemberDisconnectedEvent> captor = ArgumentCaptor.forClass(MemberDisconnectedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        MemberDisconnectedEvent event = captor.getValue();

        // disconnectMember가 member.withdraw()보다 먼저 호출되어야 익명화 전 원본 값이 이벤트에 담긴다.
        assertThat(event.memberId()).isEqualTo(10L);
        assertThat(event.provider()).isEqualTo(Provider.KAKAO);
        assertThat(event.kakaoId()).isEqualTo(123L);
        assertThat(event.appleRefreshToken()).isEqualTo("apple-refresh-token");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getKakaoId()).isNull();
        assertThat(member.getAppleRefreshToken()).isNull();
    }

    @Test
    @DisplayName("expel - 이미 WITHDRAWN 상태면 연결 해제 이벤트는 발행하되 withdraw()는 다시 수행하지 않는다")
    void expel_whenAlreadyWithdrawn_skipsWithdrawButStillDisconnects() {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao-2")
                .kakaoId(456L)
                .name("홍길동")
                .email("hong2@test.com")
                .status(MemberStatus.WITHDRAWN)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", 11L);

        memberWithdrawService.expel(member);

        then(eventPublisher).should().publishEvent(any(MemberDisconnectedEvent.class));
        // withdraw()가 호출되지 않았으므로 isDeleted/name/email 등 익명화 흔적이 없어야 한다.
        assertThat(member.isDeleted()).isFalse();
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getEmail()).isEqualTo("hong2@test.com");
        assertThat(member.getKakaoId()).isEqualTo(456L);
    }
}
