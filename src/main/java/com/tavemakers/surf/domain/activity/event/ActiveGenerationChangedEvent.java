package com.tavemakers.surf.domain.activity.event;

/**
 * 활동 기수 변경 이벤트 — 기수 변경 트랜잭션 안에서 발행된다.
 *
 * <p>member 도메인이 <b>동기 @EventListener</b>(같은 스레드·같은 트랜잭션)로 받아
 * 승인 회원의 활동 상태(YB/OB·활동 여부)를 일괄 동기화한다. 리스너에서 예외가 나면
 * 기수 변경 전체가 롤백된다 — 기수와 회원 상태는 "전부 성공 또는 전부 롤백"이어야
 * 하므로 @Async나 AFTER_COMMIT을 쓰지 않는다 (docs/refactoring-plan.md D1 결정).
 */
public record ActiveGenerationChangedEvent(Integer generation) {}
