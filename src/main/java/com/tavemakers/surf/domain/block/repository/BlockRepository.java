package com.tavemakers.surf.domain.block.repository;

import com.tavemakers.surf.domain.block.entity.Block;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface BlockRepository extends JpaRepository<Block, Long> {

    /** 내가 차단한 회원 ID 집합 — 콘텐츠 필터의 제외 대상 */
    @Query("select b.blockedId from Block b where b.blockerId = :blockerId")
    Set<Long> findBlockedIdsByBlockerId(Long blockerId);

    /** 두 회원 사이에 방향 무관하게 차단 관계가 있는지 — 쪽지·알림 등 상호작용 차단용 */
    @Query("""
           select count(b) > 0
           from Block b
           where (b.blockerId = :memberId and b.blockedId = :otherMemberId)
              or (b.blockerId = :otherMemberId and b.blockedId = :memberId)
           """)
    boolean existsBetween(Long memberId, Long otherMemberId);

    /** (나 → 대상) 단방향 차단 여부 — 중복 등록 판정·프로필 차단 표시용 */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** (나 → 대상) 단방향 차단 레코드 — 해제 대상 특정용 */
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** 내가 등록한 차단 건수 (blocker_id = me 만 센다) */
    long countByBlockerId(Long blockerId);

    /** 내가 차단한 목록 (최신순) */
    Slice<Block> findByBlockerIdOrderByCreatedAtDescIdDesc(Long blockerId, Pageable pageable);

    /** 나를 차단한 목록 (최신순) — 관리자 direction 조회용, 사용자에게 노출하지 않는다 */
    Slice<Block> findByBlockedIdOrderByCreatedAtDescIdDesc(Long blockedId, Pageable pageable);

    /** 전체 차단 관계 (최신순) — 관리자 조회용, memberId 필터가 없을 때 */
    Slice<Block> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    /** 특정 회원이 관련된 모든 차단 관계 (양방향) — 관리자 조회용 */
    @Query("""
           select b
           from Block b
           where b.blockerId = :memberId or b.blockedId = :memberId
           order by b.createdAt desc, b.id desc
           """)
    Slice<Block> findAllRelatedTo(Long memberId, Pageable pageable);

    /** 회원 hard delete(제명) 전 양방향 차단 관계 정리 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Block b where b.blockerId = :memberId or b.blockedId = :memberId")
    void deleteAllRelatedTo(Long memberId);
}
