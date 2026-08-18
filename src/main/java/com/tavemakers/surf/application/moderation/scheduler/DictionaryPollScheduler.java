package com.tavemakers.surf.application.moderation.scheduler;

import com.tavemakers.surf.application.moderation.service.DictionaryReloader;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사전 변경 폴링 — 다른 인스턴스에서 일어난 편집을 전파받는다.
 *
 * <p>AFTER_COMMIT 리스너는 편집이 일어난 인스턴스에서만 동작하므로, 나머지 인스턴스는
 * `count` + `max(updated_at)` 서명이 바뀌었는지 주기적으로 확인해 리빌드한다.
 * 현재는 단일 인스턴스 배포지만 리스너 유실 시의 안전망 역할도 겸한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryPollScheduler {

    private static final long POLL_INTERVAL_MS = 60_000L;

    private final ModerationTermRepository moderationTermRepository;
    private final DictionaryReloader dictionaryReloader;

    private volatile Signature lastSignature;

    /** 사전 서명이 직전 폴링과 다르면 스냅숏을 리빌드한다. */
    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    @Transactional(readOnly = true)
    public void pollDictionary() {
        try {
            Signature current = new Signature(
                    moderationTermRepository.count(),
                    moderationTermRepository.findMaxUpdatedAt().orElse(null));

            if (current.equals(lastSignature)) {
                return;
            }

            dictionaryReloader.reload();
            lastSignature = current;
        } catch (Exception e) {
            log.error("[MODERATION] 사전 변경 폴링에 실패했습니다.", e);
        }
    }

    /** 사전 상태 서명 — 항목 수와 최종 수정 시각이 모두 같으면 변경이 없다고 본다. */
    private record Signature(long count, LocalDateTime maxUpdatedAt) {
    }

}
