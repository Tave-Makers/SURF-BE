package com.tavemakers.surf.domain.badge.repository;

import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {

    // 특정 배지를 받은 회원 목록
    Slice<MemberBadge> findByBadgeId(Long badgeId, Pageable pageable);

    // 특정 회원의 배지 목록
    @Query("""
        select mb
        from MemberBadge mb
        join fetch mb.badge b
        where mb.member.id = :memberId
        order by mb.awardedAt desc
    """)
    List<MemberBadge> findAllWithBadgeByMemberId(
            @Param("memberId") Long memberId
    );

    // 배지 회수
    List<MemberBadge> findByBadgeIdAndMemberIdIn(Long badgeId, List<Long> memberIds);

    // 배지 삭제 전 해당 배지 부여 기록 먼저 삭제
    void deleteByBadgeId(Long badgeId);

    void deleteByMemberId(Long memberId);
}
