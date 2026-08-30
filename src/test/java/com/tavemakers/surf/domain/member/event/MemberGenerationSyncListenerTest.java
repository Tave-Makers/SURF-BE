package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.activity.event.ActiveGenerationChangedEvent;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 활동 기수 변경 이벤트 체인의 member 구간 — 승인 회원을 동기화하고,
 * 활동 회원만 담아 점수 초기화 이벤트를 이어 발행하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberGenerationSyncListenerTest {

    @Mock
    private MemberGenerationSyncService memberGenerationSyncService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberGenerationSyncListener listener;

    @Test
    @DisplayName("활동 기수 변경 시 승인 회원을 동기화하고 활동 회원만 점수 초기화 이벤트로 이어간다")
    void onActiveGenerationChanged_publishesResyncedEventWithActiveMembersOnly() {
        Member activeMember = member(true);
        Member inactiveMember = member(false);
        given(memberGenerationSyncService.syncApprovedMembersByGeneration(16))
                .willReturn(List.of(activeMember, inactiveMember));

        listener.onActiveGenerationChanged(new ActiveGenerationChangedEvent(16));

        then(memberGenerationSyncService).should().syncApprovedMembersByGeneration(16);
        then(eventPublisher).should()
                .publishEvent(new ActiveMembersResyncedEvent(List.of(activeMember)));
    }

    private Member member(boolean isActive) {
        return Member.builder()
                .name(isActive ? "active" : "inactive")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(isActive ? MemberType.YB : MemberType.OB)
                .activityStatus(isActive)
                .build();
    }
}
