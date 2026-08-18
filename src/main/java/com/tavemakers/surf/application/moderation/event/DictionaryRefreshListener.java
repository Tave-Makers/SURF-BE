package com.tavemakers.surf.application.moderation.event;

import com.tavemakers.surf.application.moderation.service.DictionaryReloader;
import com.tavemakers.surf.domain.moderation.event.ModerationDictionaryChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사전 갱신 리스너 — 관리자 편집이 커밋된 이후(AFTER_COMMIT) 스냅숏을 리빌드한다(R6).
 *
 * <p>커밋 전에 리빌드하면 롤백된 편집이 엔진에 남는다. 리빌드 실패는 전파하지 않는다 —
 * 폴링 스케줄러가 다음 주기에 같은 변경을 감지해 복구하는 안전망이 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DictionaryRefreshListener {

    private final DictionaryReloader dictionaryReloader;

    /** 사전 편집 커밋 이후 스냅숏을 교체한다. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ModerationDictionaryChangedEvent event) {
        try {
            dictionaryReloader.reload();
        } catch (Exception e) {
            log.error("[MODERATION] 사전 스냅숏 갱신 실패 — type={}, text={} (다음 폴링에서 복구된다)",
                    event.type(), event.text(), e);
        }
    }

}
