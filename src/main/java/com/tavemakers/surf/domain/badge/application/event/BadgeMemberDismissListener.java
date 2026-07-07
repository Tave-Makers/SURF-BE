package com.tavemakers.surf.domain.badge.application.event;

import com.tavemakers.surf.domain.badge.domain.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.member.event.MemberDismissedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 제명 시 badge 도메인 데이터 정리 — 제명 트랜잭션에 동기 참여한다 (D1).
 * 실패하면 제명 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class BadgeMemberDismissListener {

    private final MemberBadgeRepository memberBadgeRepository;

    /** 제명 회원의 배지 보유 기록 삭제 */
    @EventListener
    public void onMemberDismissed(MemberDismissedEvent event) {
        memberBadgeRepository.deleteByMemberId(event.memberId());
    }
}
