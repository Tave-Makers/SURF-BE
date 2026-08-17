package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.exception.TrackNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final MemberRepository memberRepository;

    /** 트랙 추가 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    public Member addTrackToMember(Long memberId, Integer generation, Part part) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.addTrack(generation, part); // Member 편의 메서드 활용
        return member;
    }

    /** 트랙 수정 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    public Track updateTrack(Long trackId, Integer generation, Part part) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        track.update(generation, part);
        return track;
    }

    /** 트랙 삭제 (관리자만 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','PRESIDENT','MANAGER')")
    public Member deleteTrack(Long trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(TrackNotFoundException::new);
        Member member = track.getMember();
        member.getTracks().remove(track);
        trackRepository.delete(track);
        return member;
    }
}
