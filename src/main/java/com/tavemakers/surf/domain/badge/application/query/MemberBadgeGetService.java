package com.tavemakers.surf.domain.badge.application.query;

import com.tavemakers.surf.domain.badge.presentation.dto.response.MemberOwnedBadgeResDTO;
import com.tavemakers.surf.domain.badge.domain.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.domain.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.domain.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import com.tavemakers.surf.domain.badge.domain.exception.BadgeNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberBadgeGetService {

    private final MemberBadgeRepository memberBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final MemberGetService memberGetService;

    /** 특정 배지를 받은 회원 목록 조회 */
    public Slice<MemberBadge> getMembersByBadge(Long badgeId, Pageable pageable) {

        // 배지 존재 여부 확인
        badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);

        return memberBadgeRepository.findByBadgeId(badgeId, pageable);
    }

    /** 특정 회원의 배지 전체 조회 */
    public List<MemberOwnedBadgeResDTO> getAllByMemberId(Long memberId) {

        // 회원 존재 여부 확인
        memberGetService.getMember(memberId);

        // 회원이 보유한 배지 목록을 fetch join으로 조회 (N+1 방지)
        List<MemberBadge> memberBadges =
                memberBadgeRepository.findAllWithBadgeByMemberId(memberId);

        return memberBadges.stream()
                .map(MemberOwnedBadgeResDTO::from)
                .toList();
    }
}