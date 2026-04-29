package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.dto.request.BadgeCreateReqDTO;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeCreateService {

    private final BadgeRepository badgeRepository;

    /** 배지 생성 */
    public Long create(BadgeCreateReqDTO request) {

        // 요청 DTO 값을 기반으로 배지 엔티티 생성
        Badge badge = new Badge(
                request.getName(),
                request.getImageUrl(),
                request.getDescription(),
                request.getRequirement()
        );

        // 배지 저장
        badgeRepository.save(badge);

        return badge.getId();
    }
}