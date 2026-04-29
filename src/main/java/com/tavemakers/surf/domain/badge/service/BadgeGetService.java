package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;

@Service
@RequiredArgsConstructor
public class BadgeGetService {

    private final BadgeRepository badgeRepository;

    /** 배지 목록 조회 (무한스크롤) */
    @Transactional(readOnly = true)
    public Slice<Badge> getBadgeList(Pageable pageable) {
        return badgeRepository.findAllBy(pageable);
    }

    /** 배지 단건 조회 */
    @Transactional(readOnly = true)
    public Badge getBadgeDetail(Long badgeId) {
        return badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
    }
}