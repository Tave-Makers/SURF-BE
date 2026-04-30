package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;

@Service
@RequiredArgsConstructor
public class BadgeDeleteService {

    private final BadgeRepository badgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;

    /** 배지 하드 삭제 */
    public void deleteBadge(Long badgeId) {

        // 삭제 대상 배지 조회
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);

        // 해당 배지 부여 기록 먼저 삭제
        memberBadgeRepository.deleteByBadgeId(badgeId);

        // 배지 삭제
        badgeRepository.delete(badge);
    }
}