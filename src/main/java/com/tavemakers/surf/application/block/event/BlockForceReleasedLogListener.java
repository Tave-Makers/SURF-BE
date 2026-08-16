package com.tavemakers.surf.application.block.event;

import com.tavemakers.surf.domain.block.event.BlockForceReleasedEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 관리자 강제 해제 감사 로그 — 커밋된 뒤에만 남긴다 (R6).
 *
 * <p>{@code LogEventEmitter.emit}은 즉시 기록하지 않고 요청 컨텍스트에 적재해 두었다가
 * 요청 종료 시 flush한다. 따라서 트랜잭션 안에서 emit하면 <b>커밋이 실패해 실제로는 아무것도
 * 해제되지 않았는데도 "강제 해제" 성공 이벤트가 그대로 남는다.</b> 감사 로그에 실패 시도가
 * 성공으로 섞이면 안 되므로 AFTER_COMMIT으로 미룬다.
 *
 * <p><b>{@code @Async}를 붙이면 안 된다.</b> 적재 대상인 {@code RequestLogContext}가 ThreadLocal이라
 * 다른 스레드에서 emit하면 요청 로그에 합류하지 못하고 조용히 사라진다.
 */
@Component
@RequiredArgsConstructor
public class BlockForceReleasedLogListener {

    private final LogEventEmitter logEventEmitter;

    /** 커밋 후 감사 로그 적재 — 누가 어떤 관계를 풀었는지 남긴다 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlockForceReleased(BlockForceReleasedEvent event) {
        logEventEmitter.emit("block_released_by_admin", Map.of(
                "admin_id", event.adminId(),
                "block_id", event.blockId(),
                "blocker_id", event.blockerId(),
                "blocked_id", event.blockedId()
        ), "관리자 차단 강제 해제");
    }
}
