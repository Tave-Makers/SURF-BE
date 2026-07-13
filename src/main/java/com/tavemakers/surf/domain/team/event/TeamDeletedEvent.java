package com.tavemakers.surf.domain.team.event;

/**
 * 팀 삭제 이벤트 — 팀 삭제 트랜잭션 안에서 발행된다.
 * 팀 단위 부속 데이터(활동기록, 팀 점수)를 소유한 도메인이
 * 동기 @EventListener(같은 트랜잭션)로 받아 정리한다 (D1 결정과 동일 패턴).
 */
public record TeamDeletedEvent(Long teamId) {}
