package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final MemberRepository memberRepository;
    private final ActiveGenerationGetService activeGenerationGetService;
    private final MemberGenerationSyncService memberGenerationSyncService;
    private final PersonalScoreCreateService personalScoreCreateService;

    /** 트랙 추가 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void addTrackToMember(Long memberId, Integer generation, Part part) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        boolean wasActive = member.isActive();
        member.addTrack(generation, part); // Member 편의 메서드 활용
        syncApprovedMemberGenerationStatus(member, wasActive, generation);
    }

    /** 트랙 수정 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void updateTrack(Long trackId, Integer generation, Part part) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        boolean wasActive = track.getMember().isActive();
        track.update(generation, part);
        syncApprovedMemberGenerationStatus(track.getMember(), wasActive, generation);
    }

    /** 트랙 삭제 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void deleteTrack(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        Member member = track.getMember();
        trackRepository.delete(track);
        syncApprovedMemberGenerationStatus(member, member.isActive(), null);
    }

    private void syncApprovedMemberGenerationStatus(Member member, boolean wasActive, Integer changedGeneration) {
        if (!member.isApproved()) {
            return;
        }

        Integer activeGeneration = activeGenerationGetService.getActiveGeneration();
        memberGenerationSyncService.syncApprovedMember(member, activeGeneration);

        if (shouldResetScore(wasActive, member, activeGeneration, changedGeneration)) {
            personalScoreCreateService.resetPersonalScores(java.util.List.of(member));
        }
    }

    private boolean shouldResetScore(boolean wasActive, Member member, Integer activeGeneration, Integer changedGeneration) {
        return !wasActive
                && member.isActive()
                && changedGeneration != null
                && activeGeneration.equals(changedGeneration);
    }
}
