package com.tavemakers.surf.domain.member.domain.event;

/**
 * 회원 제명(dismiss) 이벤트 — 제명 트랜잭션 안에서 발행된다.
 *
 * <p>각 도메인은 이 이벤트를 <b>동기 @EventListener</b>(같은 스레드·같은 트랜잭션)로 받아
 * 자기 소유의 회원 데이터를 삭제한다. 리스너에서 예외가 나면 제명 전체가 롤백된다 —
 * 계정 삭제는 "전부 성공 또는 전부 롤백"이어야 하므로 @Async나 AFTER_COMMIT을 쓰지 않는다
 * (docs/refactoring-plan.md D1 결정).
 *
 * <p>주의: 자진 탈퇴(withdraw)/퇴출(expel)에서는 발행되지 않는다 — 그 경로는 데이터를
 * 보존하는 정책이다(D2).
 */
public record MemberDismissedEvent(Long memberId) {}
