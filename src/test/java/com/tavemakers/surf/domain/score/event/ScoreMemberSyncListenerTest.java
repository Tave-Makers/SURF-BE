package com.tavemakers.surf.domain.score.event;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.event.ActiveMembersResyncedEvent;
import com.tavemakers.surf.domain.member.event.MembersApprovedEvent;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.then;

/** 회원 승인/활동 재동기화 이벤트가 score 도메인 서비스로 위임되는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class ScoreMemberSyncListenerTest {

    @Mock
    private PersonalScoreCreateService personalScoreCreateService;

    @InjectMocks
    private ScoreMemberSyncListener listener;

    @Test
    @DisplayName("회원 승인 이벤트를 받으면 개인 활동 점수를 초기 생성한다")
    void onMembersApproved_savesPersonalScores() {
        List<Member> members = List.of(member());

        listener.onMembersApproved(new MembersApprovedEvent(members));

        then(personalScoreCreateService).should().savePersonalScores(members);
    }

    @Test
    @DisplayName("활동 회원 재동기화 이벤트를 받으면 개인 활동 점수를 초기화한다")
    void onActiveMembersResynced_resetsPersonalScores() {
        List<Member> members = List.of(member());

        listener.onActiveMembersResynced(new ActiveMembersResyncedEvent(members));

        then(personalScoreCreateService).should().resetPersonalScores(members);
    }

    private Member member() {
        return Member.builder()
                .name("회원")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }
}
