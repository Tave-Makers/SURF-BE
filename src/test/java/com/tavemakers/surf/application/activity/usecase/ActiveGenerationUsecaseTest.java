package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationPutService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ActiveGenerationUsecaseTest {

    @Mock
    private ActiveGenerationGetService activeGenerationGetService;

    @Mock
    private ActiveGenerationPutService activeGenerationPutService;

    @Mock
    private MemberGenerationSyncService memberGenerationSyncService;

    @Mock
    private PersonalScoreCreateService personalScoreCreateService;

    @InjectMocks
    private ActiveGenerationUsecase activeGenerationUsecase;

    @Test
    @DisplayName("활동 기수 변경 시 활동 회원만 개인 활동 점수를 재초기화한다")
    void updateActiveGeneration_resetsPersonalScoresAfterSync() {
        Member activeMember = member(true);
        Member inactiveMember = member(false);
        List<Member> syncedMembers = List.of(activeMember, inactiveMember);
        given(memberGenerationSyncService.syncApprovedMembersByGeneration(16)).willReturn(syncedMembers);

        activeGenerationUsecase.updateActiveGeneration(16);

        then(activeGenerationPutService).should().updateActiveGeneration(16);
        then(memberGenerationSyncService).should().syncApprovedMembersByGeneration(16);
        then(personalScoreCreateService).should().resetPersonalScores(List.of(activeMember));
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
