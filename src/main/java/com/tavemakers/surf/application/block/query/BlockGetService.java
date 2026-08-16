package com.tavemakers.surf.application.block.query;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 차단 조회 진입점. 다른 도메인은 BlockRepository가 아니라 이 서비스만 호출한다(R2·R3).
 *
 * <p><b>용도별로 메서드가 나뉘어 있으며 서로 바꿔 쓰면 정책이 조용히 바뀐다.</b>
 * <ul>
 *   <li>{@link #getMyBlockedMemberIds} — JPQL {@code not in} 파라미터 전용 (게시글·댓글·스크랩)</li>
 *   <li>{@link #getMyBlockedIdsRaw} — Java {@code contains} 판정 전용 (회원 검색 isBlocked 표기)</li>
 *   <li>{@link #existsBetween} — 상호작용 차단 전용 (쪽지·개인 알림)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockGetService {

    /**
     * 빈 컬렉션을 JPQL {@code not in}에 넘기면 공급자/DB별로 렌더링이 갈려 목록이 통째로 비거나
     * 문법 오류가 난다. 차단이 0건일 때 이 값을 대신 넣어 "아무도 제외하지 않는" 조건으로 만든다.
     * 회원 ID는 IDENTITY 양수이므로 음수 sentinel은 어떤 회원과도 일치하지 않는다.
     */
    private static final Long EMPTY_SENTINEL = -1L;

    private final BlockRepository blockRepository;

    /**
     * JPQL {@code not in} 파라미터용 — <b>절대 비어 있지 않다.</b>
     * 게시글·댓글·스크랩 등 쿼리에서 행을 걸러내는 경로에서만 사용한다.
     */
    public Set<Long> getMyBlockedMemberIds(Long viewerId) {
        Set<Long> blockedIds = blockRepository.findBlockedIdsByBlockerId(viewerId);
        return blockedIds.isEmpty() ? Set.of(EMPTY_SENTINEL) : blockedIds;
    }

    /**
     * Java {@code contains} 판정용 — 비어 있을 수 있다.
     * 회원 검색의 isBlocked 표기처럼 행을 걸러내지 않고 플래그만 계산하는 경로에서만 사용한다.
     * <b>이 값을 JPQL {@code not in}에 넘기면 안 된다.</b>
     */
    public Set<Long> getMyBlockedIdsRaw(Long viewerId) {
        return blockRepository.findBlockedIdsByBlockerId(viewerId);
    }

    /**
     * 두 회원 사이에 방향 무관하게 차단 관계가 있는지.
     * 쪽지 발송·개인 알림 생성처럼 <b>상호작용</b>을 막는 경로에서만 사용한다.
     * 콘텐츠 필터에 쓰면 단방향 숨김 정책이 양방향으로 바뀐다.
     */
    public boolean existsBetween(Long memberId, Long otherMemberId) {
        return blockRepository.existsBetween(memberId, otherMemberId);
    }

    /** 내가 특정 회원을 차단했는지 (단방향) */
    public boolean isBlockedByMe(Long viewerId, Long targetId) {
        return blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId);
    }

    /** 내 차단 목록 (최신순) */
    public Slice<Block> getMyBlocks(Long blockerId, Pageable pageable) {
        return blockRepository.findByBlockerIdOrderByCreatedAtDescIdDesc(blockerId, pageable);
    }
}
