package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ActiveGenerationGetService activeGenerationGetService;

    @Mock
    private MemberGenerationSyncService memberGenerationSyncService;

    @Mock
    private PersonalScoreCreateService personalScoreCreateService;

    @InjectMocks
    private TrackService trackService;

    private Member member(Long id, MemberStatus status) {
        Member member = Member.builder()
                .name("홍길동")
                .status(status)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
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

    @Test
    @DisplayName("addTrackToMember - 회원이 없으면 예외를 던지고 어떤 동기화 로직도 호출하지 않는다")
    void addTrackToMember_whenMemberNotFound_throwsAndSkipsSync() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.addTrackToMember(1L, 17, Part.BACKEND))
                .isInstanceOf(MemberNotFoundException.class);

        then(activeGenerationGetService).shouldHaveNoInteractions();
        then(memberGenerationSyncService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addTrackToMember - 승인 회원이면 트랙을 추가하고 현재 활동 기수 기준으로 동기화한다")
    void addTrackToMember_whenApproved_addsTrackAndSyncs() {
        Member member = member(1L, MemberStatus.APPROVED);
        member.syncGenerationStatus(MemberType.OB, false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(activeGenerationGetService.getActiveGeneration()).willReturn(17);

        trackService.addTrackToMember(1L, 17, Part.BACKEND);

        assertThat(member.getTracks()).hasSize(1);
        assertThat(member.getTracks().get(0).getGeneration()).isEqualTo(17);
        assertThat(member.getTracks().get(0).getPart()).isEqualTo(Part.BACKEND);
        then(memberGenerationSyncService).should().syncApprovedMember(member, 17);
        then(personalScoreCreateService).should().resetPersonalScores(java.util.List.of(member));
    }

    @Test
    @DisplayName("addTrackToMember - 승인되지 않은 회원이면 트랙만 추가되고 동기화는 호출되지 않는다")
    void addTrackToMember_whenNotApproved_addsTrackButSkipsSync() {
        Member member = member(1L, MemberStatus.WAITING);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        trackService.addTrackToMember(1L, 17, Part.BACKEND);

        assertThat(member.getTracks()).hasSize(1);
        then(activeGenerationGetService).shouldHaveNoInteractions();
        then(memberGenerationSyncService).shouldHaveNoInteractions();
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addTrackToMember - 이미 같은 기수 트랙이 있으면 예외를 던지고 후속 로직을 호출하지 않는다")
    void addTrackToMember_whenGenerationAlreadyExists_throwsAndSkipsFollowUp() {
        Member member = member(1L, MemberStatus.WAITING);
        member.addTrack(17, Part.BACKEND);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> trackService.addTrackToMember(1L, 17, Part.DEEP_LEARNING))
                .isInstanceOf(com.tavemakers.surf.domain.member.exception.TrackAlreadyExistsException.class);

        then(activeGenerationGetService).shouldHaveNoInteractions();
        then(memberGenerationSyncService).shouldHaveNoInteractions();
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("addTrackToMember - 현재 활동 기수가 아닌 트랙 추가는 점수를 초기화하지 않는다")
    void addTrackToMember_whenNotCurrentGeneration_doesNotResetScore() {
        Member member = member(1L, MemberStatus.APPROVED);
        member.syncGenerationStatus(MemberType.OB, false);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(activeGenerationGetService.getActiveGeneration()).willReturn(17);

        trackService.addTrackToMember(1L, 16, Part.BACKEND);

        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateTrack - 트랙이 없으면 예외를 던진다")
    void updateTrack_whenTrackNotFound_throws() {
        given(trackRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.updateTrack(5L, 17, Part.BACKEND))
                .isInstanceOf(TrackNotFoundException.class);

        then(memberGenerationSyncService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateTrack - 승인 회원의 트랙이면 값을 수정하고 소유 회원을 동기화한다")
    void updateTrack_whenOwnerApproved_updatesAndSyncs() {
        Member owner = member(2L, MemberStatus.APPROVED);
        owner.syncGenerationStatus(MemberType.OB, false);
        Track track = track(5L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(track));
        given(activeGenerationGetService.getActiveGeneration()).willReturn(16);

        trackService.updateTrack(5L, 16, Part.WEB_FRONTEND);

        assertThat(track.getGeneration()).isEqualTo(16);
        assertThat(track.getPart()).isEqualTo(Part.WEB_FRONTEND);
        then(memberGenerationSyncService).should().syncApprovedMember(owner, 16);
        then(personalScoreCreateService).should().resetPersonalScores(java.util.List.of(owner));
    }

    @Test
    @DisplayName("updateTrack - 승인되지 않은 회원의 트랙이면 값만 수정되고 동기화는 호출되지 않는다")
    void updateTrack_whenOwnerNotApproved_updatesButSkipsSync() {
        Member owner = member(2L, MemberStatus.WAITING);
        Track track = track(5L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(track));

        trackService.updateTrack(5L, 16, Part.WEB_FRONTEND);

        assertThat(track.getGeneration()).isEqualTo(16);
        then(activeGenerationGetService).shouldHaveNoInteractions();
        then(memberGenerationSyncService).shouldHaveNoInteractions();
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateTrack - 이미 활동 중인 회원의 현재 활동 기수 트랙 수정은 점수를 다시 초기화하지 않는다")
    void updateTrack_whenAlreadyActive_doesNotResetScore() {
        Member owner = member(2L, MemberStatus.APPROVED);
        Track oldTrack = track(5L, owner, 16, Part.BACKEND);
        owner.addTrack(15, Part.WEB_FRONTEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(oldTrack));
        given(activeGenerationGetService.getActiveGeneration()).willReturn(16);

        trackService.updateTrack(5L, 16, Part.DEEP_LEARNING);

        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("deleteTrack - 트랙이 없으면 예외를 던진다")
    void deleteTrack_whenTrackNotFound_throws() {
        given(trackRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.deleteTrack(7L))
                .isInstanceOf(TrackNotFoundException.class);

        then(memberGenerationSyncService).shouldHaveNoInteractions();
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("deleteTrack - 승인 회원의 트랙이면 삭제 후 소유 회원을 동기화한다")
    void deleteTrack_whenOwnerApproved_deletesAndSyncs() {
        Member owner = member(3L, MemberStatus.APPROVED);
        Track track = track(7L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(7L)).willReturn(Optional.of(track));
        given(activeGenerationGetService.getActiveGeneration()).willReturn(20);

        trackService.deleteTrack(7L);

        then(trackRepository).should().delete(track);
        then(memberGenerationSyncService).should().syncApprovedMember(owner, 20);
        then(personalScoreCreateService).shouldHaveNoInteractions();
    }
}
