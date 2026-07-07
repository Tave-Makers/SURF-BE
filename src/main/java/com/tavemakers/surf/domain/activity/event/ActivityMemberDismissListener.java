package com.tavemakers.surf.domain.activity.event;

import com.tavemakers.surf.domain.activity.repository.ActivityRecordRepository;
import com.tavemakers.surf.domain.member.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.team.event.TeamDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 제명/팀 삭제 시 activity 도메인 데이터 정리 — 발행자 트랜잭션에 동기 참여한다 (D1).
 * 실패하면 제명 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ActivityMemberDismissListener {

    private final ActivityRecordRepository activityRecordRepository;

    /** 제명 회원의 활동기록 삭제 */
    @EventListener
    public void onMemberDismissed(MemberDismissedEvent event) {
        activityRecordRepository.deleteByMemberId(event.memberId());
    }

    /** 삭제되는 팀의 활동기록 삭제 */
    @EventListener
    public void onTeamDeleted(TeamDeletedEvent event) {
        activityRecordRepository.deleteByTeamId(event.teamId());
    }
}
