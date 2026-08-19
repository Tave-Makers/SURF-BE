package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.exception.TrackAlreadyExistsException;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
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
    }

    @Test
    @DisplayName("addTrackToMember - 승인 회원이면 트랙을 추가하고 회원을 반환한다")
    void addTrackToMember_whenApproved_addsTrackAndReturnsMember() {
        Member member = member(1L, MemberStatus.APPROVED);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Member result = trackService.addTrackToMember(1L, 17, Part.BACKEND);

        assertThat(result).isSameAs(member);
        assertThat(member.getTracks()).hasSize(1);
        assertThat(member.getTracks().get(0).getGeneration()).isEqualTo(17);
        assertThat(member.getTracks().get(0).getPart()).isEqualTo(Part.BACKEND);
    }

    @Test
    @DisplayName("addTrackToMember - 승인되지 않은 회원도 트랙은 추가된다")
    void addTrackToMember_whenNotApproved_addsTrack() {
        Member member = member(1L, MemberStatus.WAITING);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        trackService.addTrackToMember(1L, 17, Part.BACKEND);

        assertThat(member.getTracks()).hasSize(1);
    }

    @Test
    @DisplayName("addTrackToMember - 이미 같은 기수 트랙이 있으면 예외를 던지고 후속 로직을 호출하지 않는다")
    void addTrackToMember_whenGenerationAlreadyExists_throwsAndSkipsFollowUp() {
        Member member = member(1L, MemberStatus.WAITING);
        member.addTrack(17, Part.BACKEND);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> trackService.addTrackToMember(1L, 17, Part.DEEP_LEARNING))
                .isInstanceOf(com.tavemakers.surf.domain.member.exception.TrackAlreadyExistsException.class);
    }

    @Test
    @DisplayName("updateTrack - 트랙이 없으면 예외를 던진다")
    void updateTrack_whenTrackNotFound_throws() {
        given(trackRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.updateTrack(5L, 17, Part.BACKEND))
                .isInstanceOf(TrackNotFoundException.class);
    }

    @Test
    @DisplayName("updateTrack - 트랙 값을 수정하고 수정된 트랙을 반환한다")
    void updateTrack_whenOwnerApproved_updatesAndReturnsTrack() {
        Member owner = member(2L, MemberStatus.APPROVED);
        Track track = track(5L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(track));

        Track result = trackService.updateTrack(5L, 16, Part.WEB_FRONTEND);

        assertThat(result).isSameAs(track);
        assertThat(track.getGeneration()).isEqualTo(16);
        assertThat(track.getPart()).isEqualTo(Part.WEB_FRONTEND);
    }

    @Test
    @DisplayName("updateTrack - 승인되지 않은 회원의 트랙도 값은 수정된다")
    void updateTrack_whenOwnerNotApproved_updates() {
        Member owner = member(2L, MemberStatus.WAITING);
        Track track = track(5L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(track));

        trackService.updateTrack(5L, 16, Part.WEB_FRONTEND);

        assertThat(track.getGeneration()).isEqualTo(16);
    }

    @Test
    @DisplayName("updateTrack - 같은 회원의 다른 트랙과 기수가 중복되면 예외를 던진다")
    void updateTrack_whenGenerationAlreadyExists_throws() {
        Member owner = member(2L, MemberStatus.APPROVED);
        owner.addTrack(24, Part.BACKEND);
        Track targetTrack = track(5L, owner, 25, Part.WEB_FRONTEND);
        given(trackRepository.findById(5L)).willReturn(Optional.of(targetTrack));

        assertThatThrownBy(() -> trackService.updateTrack(5L, 24, Part.DEEP_LEARNING))
                .isInstanceOf(TrackAlreadyExistsException.class);
    }

    @Test
    @DisplayName("deleteTrack - 트랙이 없으면 예외를 던진다")
    void deleteTrack_whenTrackNotFound_throws() {
        given(trackRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.deleteTrack(7L))
                .isInstanceOf(TrackNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTrack - 트랙을 삭제하고 소유 회원을 반환한다")
    void deleteTrack_whenOwnerApproved_deletesAndReturnsOwner() {
        Member owner = member(3L, MemberStatus.APPROVED);
        Track track = track(7L, owner, 15, Part.BACKEND);
        given(trackRepository.findById(7L)).willReturn(Optional.of(track));

        Member result = trackService.deleteTrack(7L);

        assertThat(result).isSameAs(owner);
        assertThat(owner.getTracks()).doesNotContain(track);
        then(trackRepository).should().delete(track);
    }
}
