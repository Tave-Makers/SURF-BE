package com.tavemakers.surf.domain.badge.application.query;

import com.tavemakers.surf.domain.badge.domain.entity.Badge;
import com.tavemakers.surf.domain.badge.domain.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import com.tavemakers.surf.domain.badge.domain.exception.BadgeNotFoundException;

@Service
@RequiredArgsConstructor
public class BadgeGetService {

    private final BadgeRepository badgeRepository;

    /** 배지 목록 조회 (무한스크롤) */
    public Slice<Badge> getBadgeList(Pageable pageable) {
        return badgeRepository.findAllBy(pageable);
    }

    /** 배지 단건 조회 */
    public Badge getBadgeDetail(Long badgeId) {
        return badgeRepository.findById(badgeId)
                .orElseThrow(BadgeNotFoundException::new);
    }
}