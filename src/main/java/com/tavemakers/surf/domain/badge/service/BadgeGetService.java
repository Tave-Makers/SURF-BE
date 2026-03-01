package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeGetService {

    private final BadgeRepository badgeRepository;

    /** 배지 목록 조회 (무한스크롤) */
    @Transactional(readOnly = true)
    public Slice<Badge> getBadgeList(Pageable pageable) {
        return badgeRepository.findAllBy(pageable);
    }
}