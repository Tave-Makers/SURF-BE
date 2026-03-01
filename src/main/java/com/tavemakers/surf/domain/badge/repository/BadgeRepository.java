package com.tavemakers.surf.domain.badge.repository;

import com.tavemakers.surf.domain.badge.entity.Badge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    // 전체 배지 무한스크롤 조회 (20개씩)
    Slice<Badge> findAllBy(Pageable pageable);
}