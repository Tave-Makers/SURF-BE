package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.member.entity.Member;

import java.util.List;

/**
 * 활동 회원 재동기화 이벤트 — 활동 기수 변경 또는 트랙 변경으로 활동 상태가 갱신된
 * 회원들을 담아 동기화 트랜잭션 안에서 발행된다.
 *
 * <p>score 도메인이 <b>동기 @EventListener</b>(같은 스레드·같은 트랜잭션)로 받아
 * 개인 활동 점수를 현재 회원 구분(YB/OB)에 맞게 초기화한다. 영속 상태의 엔티티를
 * 그대로 담으므로 반드시 동기 리스너로만 소비해야 한다 — @Async/AFTER_COMMIT로
 * 옮기면 detached 엔티티가 되어 깨진다 (docs/refactoring-plan.md D1 결정).
 *
 * @param activeMembers 점수 초기화 대상 활동 회원들 (비활동 회원은 담지 않는다)
 */
public record ActiveMembersResyncedEvent(List<Member> activeMembers) {}
