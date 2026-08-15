package com.tavemakers.surf.application.block.query;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.entity.enums.BlockDirection;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 차단 관계 조회. 사용자 조회({@link BlockGetService})와 달리 방향 제한 없이 양쪽을 다 본다.
 *
 * <p>사용자 경로에서 이 서비스를 쓰면 "나를 차단한 사람"이 노출되므로 분리해 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockAdminGetService {

    private final BlockRepository blockRepository;

    /**
     * 차단 관계 목록.
     *
     * <p>{@code memberId}가 없으면 전체를 최신순으로 본다. {@code direction}은 {@code memberId}가
     * 있을 때만 의미가 있으며, 없으면 {@link BlockDirection#ALL}(양방향)로 취급한다.
     */
    public Slice<Block> getBlocks(Long memberId, BlockDirection direction, Pageable pageable) {
        if (memberId == null) {
            return blockRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        }

        BlockDirection resolved = (direction == null) ? BlockDirection.ALL : direction;
        return switch (resolved) {
            case BLOCKING -> blockRepository.findByBlockerIdOrderByCreatedAtDescIdDesc(memberId, pageable);
            case BLOCKED -> blockRepository.findByBlockedIdOrderByCreatedAtDescIdDesc(memberId, pageable);
            case ALL -> blockRepository.findAllRelatedTo(memberId, pageable);
        };
    }
}
