package com.tavemakers.surf.application.block.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.service.BlockCreateService;
import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.presentation.block.dto.response.BlockSliceResDTO;
import com.tavemakers.surf.presentation.block.dto.response.BlockedMemberResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 사용자 차단 등록·해제·목록 조회. 트랜잭션 경계를 소유한다 (R4).
 */
@Service
@RequiredArgsConstructor
public class BlockUsecase {

    private final BlockCreateService blockCreateService;
    private final BlockDeleteService blockDeleteService;
    private final BlockGetService blockGetService;
    private final MemberGetService memberGetService;

    /**
     * 차단 등록.
     *
     * <p>대상 회원 검증을 insert보다 먼저 한다. 운영 FK가 RESTRICT라 없는 회원으로 insert하면
     * 409가 아니라 FK 위반이 되고, 사용자에게는 원인을 알 수 없는 오류가 된다.
     *
     * <p>자기 차단(400)은 {@code Block.of}가, 중복(409)은 {@code BlockCreateService}가 판정한다.
     * 여기서 다시 검사하면 규칙이 두 곳으로 흩어지므로 위임한다.
     */
    @Transactional
    @LogEvent(value = "block.create", message = "차단 등록")
    public BlockedMemberResDTO create(Long blockerId, @LogParam("blocked_id") Long blockedId) {
        Member target = getActiveMember(blockedId);

        Block block = blockCreateService.create(blockerId, blockedId);

        return BlockedMemberResDTO.of(target, block.getCreatedAt());
    }

    /**
     * 차단 해제 — (나 → 대상) 방향만 지운다.
     * 상대가 나를 차단한 레코드는 내가 풀 수 있는 대상이 아니므로 건드리지 않는다.
     */
    @Transactional
    @LogEvent(value = "block.delete", message = "차단 해제")
    public void delete(Long blockerId, @LogParam("blocked_id") Long blockedId) {
        blockDeleteService.delete(blockerId, blockedId);
    }

    /**
     * 내 차단 목록 (최신순).
     *
     * <p>{@code blocker_id = me}만 조회한다 — 나를 차단한 회원은 사용자 화면에 노출하지 않는다.
     * 회원 정보는 페이지 단위로 한 번에 조회해 N+1을 막는다.
     */
    @Transactional(readOnly = true)
    public BlockSliceResDTO getMyBlocks(Long blockerId, Pageable pageable) {
        Slice<Block> blocks = blockGetService.getMyBlocks(blockerId, pageable);

        Map<Long, Member> members = findMembersById(
                blocks.getContent().stream().map(Block::getBlockedId).collect(Collectors.toSet()));

        return BlockSliceResDTO.from(blocks.map(
                block -> BlockedMemberResDTO.of(
                        requireMember(members, block.getBlockedId()), block.getCreatedAt())));
    }

    /** 차단 대상 검증 — 없거나 탈퇴한 회원은 차단할 수 없다 */
    private Member getActiveMember(Long memberId) {
        Member member = memberGetService.getMember(memberId);
        if (member.isDeleted()) {
            throw new MemberNotFoundException();
        }
        return member;
    }

    private Map<Long, Member> findMembersById(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        List<Member> members = memberGetService.getMembers(memberIds);
        return members.stream().collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    /**
     * 운영 FK(RESTRICT)와 제명 정리 리스너가 있어 차단 관계의 상대 회원은 항상 존재한다.
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
