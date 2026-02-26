package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.exception.MemberBadgeNotFoundException;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberBadgeRevokeService {

    private final MemberBadgeRepository memberBadgeRepository;
    private final BadgeRepository badgeRepository;

    /** 배지 회수 */
    @Transactional
    public void revoke(Long badgeId, List<Long> memberIds) {

        // 배지 존재 여부 먼저 확인
        badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);

        // 회수 대상 배지-회원 매핑 조회
        List<MemberBadge> memberBadges =
                memberBadgeRepository.findByBadgeIdAndMemberIdIn(badgeId, memberIds);

        // 요청 수와 조회 결과 수가 다르면 일부는 존재하지 않는 매핑 → 예외
        if (memberBadges.size() != memberIds.size()) {
            throw new MemberBadgeNotFoundException();
        }

        // 해당 배지-회원 매핑 전체 삭제
        memberBadgeRepository.deleteAll(memberBadges);
    }
}