package com.tavemakers.surf.domain.block.event;

/**
 * 관리자가 차단 관계를 강제 해제했음을 알리는 이벤트.
 *
 * <p>삭제된 레코드의 blocker/blocked는 커밋 후에는 조회할 수 없으므로 이벤트에 실어 보낸다.
 */
public record BlockForceReleasedEvent(
        Long adminId,
        Long blockId,
        Long blockerId,
        Long blockedId
) {
}
