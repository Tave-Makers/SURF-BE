package com.tavemakers.surf.application.member.usecase;

import java.time.LocalDateTime;

/** 발급된 통합 토큰 스냅샷 — 발급 트랜잭션 밖으로 토큰·만료 시각만 넘긴다. 호출부는 expiresAt으로 실제 잔여 TTL을 계산한다. */
public record IssuedIntegrationToken(String token, LocalDateTime expiresAt) {
}
