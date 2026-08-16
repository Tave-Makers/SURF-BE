package com.tavemakers.surf.application.block.event;

import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.event.MemberDismissedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 제명 시 차단 관계 정리 — 발행자 트랜잭션에 동기 참여한다 (D1).
 *
 * <p>운영 스키마의 {@code fk_block_blocker}/{@code fk_block_blocked}는 RESTRICT이므로,
 * 이 정리가 빠지면 회원 hard delete 자체가 FK 위반으로 실패한다. 즉 이 리스너는 부수효과가 아니라
 * 제명의 선행 조건이다.
 *
 * <p>따라서 {@code @EventListener}로 같은 트랜잭션에 참여하며 {@code @Async}·AFTER_COMMIT을 쓰지 않는다.
 * 정리에 실패하면 제명 전체가 롤백되어야 한다(R6의 AFTER_COMMIT 규칙은 커밋 후에 해도 되는 비동기
 * 부수효과에 대한 것이고, 이 리스너는 그 범주가 아니다).
 */
@Component
@RequiredArgsConstructor
public class BlockMemberDismissListener {

    private final BlockDeleteService blockDeleteService;

    /** 제명 회원이 관련된 양방향 차단 관계 삭제 */
    @EventListener
    public void onMemberDismissed(MemberDismissedEvent event) {
        blockDeleteService.deleteAllRelatedTo(event.memberId());
    }
}
