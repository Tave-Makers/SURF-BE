package com.tavemakers.surf.domain.member.event;

/**
 * 계정 통합 완료 이벤트 — 통합 성공 시 발행.
 * 임시 REGISTERING 회원의 refresh 토큰 무효화를 커밋 이후로 미루기 위한 이벤트다. (§3.6)
 */
public record SocialAccountIntegratedEvent(Long tempMemberId) {}
