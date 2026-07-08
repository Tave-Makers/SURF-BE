package com.tavemakers.surf.domain.post.domain.event;

import com.tavemakers.surf.domain.member.domain.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.post.domain.service.search.RecentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 제명 시 최근 검색 기록(Redis) 정리 — DB 트랜잭션과 무관한 외부 저장소이므로
 * 동기 리스너가 아니라 커밋 후(AFTER_COMMIT) 비동기로 수행한다.
 * 제명이 롤백되면 검색 기록은 남아야 하고, Redis 정리 실패가 제명을 막아서도 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentSearchCleanupListener {

    private final RecentSearchService recentSearchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberDismissed(MemberDismissedEvent event) {
        try {
            recentSearchService.clearAll(event.memberId());
        } catch (Exception e) {
            log.error("[DISMISS] 최근 검색 기록 정리 실패 memberId={}", event.memberId(), e);
        }
    }
}
