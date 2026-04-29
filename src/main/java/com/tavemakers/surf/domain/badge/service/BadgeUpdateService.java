package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.dto.request.BadgeUpdateReqDTO;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;

@Service
@RequiredArgsConstructor
public class BadgeUpdateService {

    private final BadgeRepository badgeRepository;

    /** 배지 정보 수정 */
    public void update(Long badgeId, BadgeUpdateReqDTO request) {

        // 수정 대상 배지 조회 (없으면 예외)
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);

        // 요청 값으로 배지 필드 업데이트
        badge.update(
                request.getName(),
                request.getImageUrl(),
                request.getDescription(),
                request.getRequirement()
        );
    }
}