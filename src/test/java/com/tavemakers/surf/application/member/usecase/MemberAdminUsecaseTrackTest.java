package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.application.member.query.CareerGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.member.query.TrackGetService;
import com.tavemakers.surf.application.score.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.service.MemberDismissService;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import com.tavemakers.surf.domain.member.service.MemberPatchService;
import com.tavemakers.surf.domain.member.service.MemberWithdrawService;
import com.tavemakers.surf.domain.member.service.TrackService;
import com.tavemakers.surf.domain.member.validator.RoleChangeValidator;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberAdminUsecaseTrackTest {

    @Mock private MemberPatchService memberPatchService;
    @Mock private MemberGetService memberGetService;
    @Mock private ActiveGenerationGetService activeGenerationGetService;
    @Mock private MemberGenerationSyncService memberGenerationSyncService;
    @Mock private MemberBlacklistCreateService memberBlacklistCreateService;
    @Mock private MemberDismissService memberDismissService;
    @Mock private MemberDismissUsecase memberDismissUsecase;
    @Mock private CareerGetService careerGetService;
    @Mock private PersonalScoreCreateService personalScoreCreateService;
    @Mock private PersonalScoreGetService scoreGetService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private TrackGetService trackGetService;
    @Mock private TrackService trackService;
    @Mock private MemberWithdrawService memberWithdrawService;
    @Mock private LogEventEmitter logEventEmitter;
    @Mock private RoleChangeValidator roleChangeValidator;

    @InjectMocks
    private MemberAdminUsecase memberAdminUsecase;

    @Test
    @DisplayName("addTrack - 비활동 승인 회원이 현재 활동기수 트랙을 추가받으면 상태 동기화 후 점수를 초기화한다")
    void addTrack_whenInactiveApprovedMemberGetsCurrentGeneration_resetsScore() {
        Member member = member(1L, MemberStatus.APPROVED, MemberType.OB, false);
        given(memberGetService.getMember(1L)).willReturn(member);
        given(activeGenerationGetService.getActiveGeneration()).willReturn(25);

        given(trackService.addTrackToMember(1L, 25, Part.BACKEND)).willAnswer(invocation -> {
            member.addTrack(25, Part.BACKEND);
            member.syncGenerationStatus(MemberType.YB, true);
            return member;
        });

        memberAdminUsecase.addTrack(1L, 25, Part.BACKEND);

        then(memberGenerationSyncService).should().syncApprovedMember(member, 25);
        then(personalScoreCreateService).should().resetPersonalScores(List.of(member));
    }

    @Test
    @DisplayName("updateTrack - 이미 활동 중인 회원의 part 수정은 상태만 동기화하고 점수는 초기화하지 않는다")
    void updateTrack_whenAlreadyActiveAndPartOnlyChanged_doesNotResetScore() {
        Member member = member(2L, MemberStatus.APPROVED, MemberType.YB, true);
        Track track = track(10L, member, 25, Part.BACKEND);
        given(trackGetService.findTrackById(10L)).willReturn(track);
        given(activeGenerationGetService.getActiveGeneration()).willReturn(25);
        given(trackService.updateTrack(10L, null, Part.WEB_FRONTEND)).willAnswer(invocation -> {
            track.update(null, Part.WEB_FRONTEND);
            return track;
        });

        memberAdminUsecase.updateTrack(10L, null, Part.WEB_FRONTEND);

        then(memberGenerationSyncService).should().syncApprovedMember(member, 25);
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    private Member member(Long id, MemberStatus status, MemberType memberType, boolean isActive) {
        Member member = Member.builder()
                .name("회원" + id)
                .status(status)
                .role(MemberRole.MEMBER)
                .memberType(memberType)
                .activityStatus(isActive)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Track track(Long id, Member owner, Integer generation, Part part) {
        Track track = new Track(generation, part);
        track.setMember(owner);
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }
}
