package com.tavemakers.surf.domain.score.event;

import com.tavemakers.surf.domain.member.event.ActiveMembersResyncedEvent;
import com.tavemakers.surf.domain.member.event.MembersApprovedEvent;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 승인/활동 상태 동기화 시 score 도메인 후속 처리 — 발행자 트랜잭션에 동기 참여한다 (D1).
 * 점수 생성·초기화는 회원 상태 변경과 "전부 성공 또는 전부 롤백"이어야 하므로
 * @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ScoreMemberSyncListener {

    private final PersonalScoreCreateService personalScoreCreateService;

    /** 승인된 회원들의 개인 활동 점수 초기 생성 */
    @EventListener
    public void onMembersApproved(MembersApprovedEvent event) {
        personalScoreCreateService.savePersonalScores(event.members());
    }

    /** 활동 회원으로 재동기화된 회원들의 개인 활동 점수 초기화 */
    @EventListener
    public void onActiveMembersResynced(ActiveMembersResyncedEvent event) {
        personalScoreCreateService.resetPersonalScores(event.activeMembers());
    }
}
