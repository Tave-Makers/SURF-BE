package com.tavemakers.surf.application.auth.common.event;

import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.event.SocialAccountIntegratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 계정 통합 후처리(auth) — 커밋 이후(AFTER_COMMIT) 임시 REGISTERING 회원의 refresh 토큰을 무효화한다 (§3.6, R6).
 * 트랜잭션 안에서 무효화하면 롤백 시 되돌릴 수 없으므로 커밋 이후로 미룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialAccountIntegratedListener {

    private final RefreshTokenService refreshTokenService;

    /** 통합 완료 후 임시 회원의 refresh token을 전부 무효화한다. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SocialAccountIntegratedEvent event) {
        refreshTokenService.invalidateAll(event.tempMemberId());
    }
}
