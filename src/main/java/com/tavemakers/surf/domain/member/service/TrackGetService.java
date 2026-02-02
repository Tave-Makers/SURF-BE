package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackGetService {

    private final TrackRepository trackRepository;

    //유저의 가장 최신 기수 트랙을 가져옴
    public List<Track> getTrack(List<Long> memberIds) {
        return trackRepository.findLatestTracksByMemberIds(memberIds);
    }

    /** 회원 ID로 트랙 목록 조회 */
    public List<Track> getTrack(Long memberId) {
        return trackRepository.findByMemberId(memberId);
    }

    /** 회원 ID로 기수순 정렬된 트랙 목록 조회 */
    public List<Track> getTrackSortedByGeneration(Long memberId) {
        return trackRepository.findByMemberId(memberId)
                .stream()
                .sorted()
                .toList();
    }

    //트랙과 함께 모든 회원 반환
    public List<Track> getAllTracksWithMember() {
        return trackRepository.findAllWithActiveMember();
    }

    public List<Integer> getExistsAllGenerations() {
        return trackRepository.findAllDistinctGenerations();
    }

}
