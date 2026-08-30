package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.member.entity.Member;

import java.util.List;

/**
 * 회원가입 승인 이벤트 — 승인 트랜잭션 안에서 발행된다.
 *
 * <p>score 도메인이 <b>동기 @EventListener</b>(같은 스레드·같은 트랜잭션)로 받아
 * 개인 활동 점수를 초기 생성한다. 승인과 점수 생성은 "전부 성공 또는 전부 롤백"이어야
 * 하고, 영속 상태의 엔티티를 그대로 담으므로 @Async/AFTER_COMMIT을 쓰지 않는다
 * (docs/refactoring-plan.md D1 결정).
 */
public record MembersApprovedEvent(List<Member> members) {}
