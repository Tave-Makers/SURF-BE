package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.exception.BlockNotFoundException;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 차단 해제 도메인 로직. 트랜잭션 경계는 호출자(BlockUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class BlockDeleteService {

    private final BlockRepository blockRepository;

    /**
     * 차단 해제. 반드시 (blocker → blocked) 방향만 삭제하며 반대 방향 레코드는 남긴다.
     * 상대가 나를 차단한 사실은 내가 해제할 수 있는 대상이 아니다.
     */
    public Block delete(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(BlockNotFoundException::new);
        blockRepository.delete(block);
        return block;
    }

    /** 회원 제명(hard delete) 시 해당 회원이 관련된 양방향 차단 관계를 모두 제거 */
    public void deleteAllRelatedTo(Long memberId) {
        blockRepository.deleteAllRelatedTo(memberId);
    }
}
