package com.tavemakers.surf.domain.member.event;

import com.tavemakers.surf.domain.activity.event.ActiveGenerationChangedEvent;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 활동 기수 변경 시 member 도메인 동기화 — 발행자 트랜잭션에 동기 참여한다 (D1).
 * 실패하면 기수 변경 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class MemberGenerationSyncListener {

    private final MemberGenerationSyncService memberGenerationSyncService;
    private final ApplicationEventPublisher eventPublisher;

    /** 승인 회원 활동 상태 일괄 동기화 후, 활동 회원의 점수 초기화를 이벤트로 이어간다 */
    @EventListener
    public void onActiveGenerationChanged(ActiveGenerationChangedEvent event) {
        List<Member> syncedMembers =
                memberGenerationSyncService.syncApprovedMembersByGeneration(event.generation());

        eventPublisher.publishEvent(new ActiveMembersResyncedEvent(
                syncedMembers.stream()
                        .filter(Member::isActive)
                        .toList()
        ));
    }
}
