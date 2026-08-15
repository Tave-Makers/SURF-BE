package com.tavemakers.surf.application.block.usecase;

import com.tavemakers.surf.application.block.query.BlockAdminGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.entity.enums.BlockDirection;
import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.block.dto.response.BlockAdminResDTO;
import com.tavemakers.surf.presentation.block.dto.response.BlockAdminSliceResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 차단 관계 조회·강제 해제. 트랜잭션 경계를 소유한다 (R4).
 */
@Service
@RequiredArgsConstructor
public class BlockAdminUsecase {

    private final BlockAdminGetService blockAdminGetService;
    private final BlockDeleteService blockDeleteService;
    private final MemberGetService memberGetService;
    private final LogEventEmitter logEventEmitter;

    /**
     * 차단 관계 목록. 양쪽 회원을 페이지 단위로 한 번에 조회해 N+1을 막는다.
     */
    @Transactional(readOnly = true)
    public BlockAdminSliceResDTO getBlocks(Long memberId, BlockDirection direction, Pageable pageable) {
        Slice<Block> blocks = blockAdminGetService.getBlocks(memberId, direction, pageable);

        Map<Long, Member> members = findMembersById(collectMemberIds(blocks.getContent()));

        return BlockAdminSliceResDTO.from(blocks.map(
                block -> BlockAdminResDTO.of(
                        block,
                        requireMember(members, block.getBlockerId()),
                        requireMember(members, block.getBlockedId()))));
    }

    /**
     * 관리자 강제 해제. 사용자 해제와 달리 방향을 따지지 않고 block_id로 지운다.
     *
     * <p>남의 차단 관계를 임의로 푸는 동작이라 누가 무엇을 풀었는지 감사 로그를 남긴다.
     * 삭제된 레코드에서 blocker/blocked를 읽어야 하므로 어노테이션이 아니라 emitter로 기록한다.
     */
    @Transactional
    public void forceDelete(Long adminId, Long blockId) {
        Block block = blockDeleteService.deleteById(blockId);

        logEventEmitter.emit("block_released_by_admin", Map.of(
                "admin_id", adminId,
                "block_id", blockId,
                "blocker_id", block.getBlockerId(),
                "blocked_id", block.getBlockedId()
        ), "관리자 차단 강제 해제");
    }

    /** 한 페이지에 등장하는 blocker·blocked를 한 번에 조회하기 위해 ID를 모은다 */
    private Set<Long> collectMemberIds(List<Block> blocks) {
        Set<Long> memberIds = new HashSet<>();
        for (Block block : blocks) {
            memberIds.add(block.getBlockerId());
            memberIds.add(block.getBlockedId());
        }
        return memberIds;
    }

    private Map<Long, Member> findMembersById(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        List<Member> members = memberGetService.getMembers(memberIds);
        return members.stream().collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    /**
     * 운영 FK(RESTRICT)와 제명 정리 리스너가 있어 차단 관계의 양쪽 회원은 항상 존재한다.
     * 없다면 정리 누락이므로 빈 값으로 감추지 않고 드러낸다.
     */
    private Member requireMember(Map<Long, Member> members, Long memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new MemberNotFoundException();
        }
        return member;
    }
}
