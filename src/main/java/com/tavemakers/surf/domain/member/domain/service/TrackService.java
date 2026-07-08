package com.tavemakers.surf.domain.member.domain.service;

import com.tavemakers.surf.domain.activity.application.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.Track;
import com.tavemakers.surf.domain.member.domain.entity.enums.Part;
import com.tavemakers.surf.domain.member.domain.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.domain.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.domain.repository.MemberRepository;
import com.tavemakers.surf.domain.member.domain.repository.TrackRepository;
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

    /** 트랙 추가 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void addTrackToMember(Long memberId, Integer generation, Part part) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.addTrack(generation, part); // Member 편의 메서드 활용
        syncApprovedMemberGenerationStatus(member);
    }

    /** 트랙 수정 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void updateTrack(Long trackId, Integer generation, Part part) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        track.update(generation, part);
        syncApprovedMemberGenerationStatus(track.getMember());
    }

    /** 트랙 삭제 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    @Transactional
    public void deleteTrack(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        Member member = track.getMember();
        trackRepository.delete(track);
        syncApprovedMemberGenerationStatus(member);
    }

    private void syncApprovedMemberGenerationStatus(Member member) {
        if (!member.isApproved()) {
            return;
        }

        Integer activeGeneration = activeGenerationGetService.getActiveGeneration();
        memberGenerationSyncService.syncApprovedMember(member, activeGeneration);
    }
}
