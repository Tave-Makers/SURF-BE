package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.presentation.activity.dto.activeGeneration.response.ActiveGenerationMemberResDTO;
import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.activity.event.ActiveGenerationChangedEvent;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationPutService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 활동 기수 Usecase */
@Service
@RequiredArgsConstructor
public class ActiveGenerationUsecase {

    private final ActiveGenerationGetService activeGenerationGetService;
    private final ActiveGenerationPutService activeGenerationPutService;
    private final ApplicationEventPublisher eventPublisher;

    /** 현재 활동 기수 조회 */
    @Transactional(readOnly = true)
    public Integer getActiveGeneration() {
        return activeGenerationGetService.getActiveGeneration();
    }

    /** 활동 기수 회원 목록 조회 */
    @Transactional(readOnly = true)
    public List<ActiveGenerationMemberResDTO> getActiveGenerationMembers() {
        return activeGenerationGetService.getActiveGenerationMembers();
    }

    /**
     * 활동 기수 변경 — 회원 상태 동기화와 점수 초기화는 동기 이벤트 체인으로 이어진다
     * (member: {@code MemberGenerationSyncListener} → score: {@code ScoreMemberSyncListener}).
     * 리스너가 같은 트랜잭션에 참여하므로 실패 시 기수 변경까지 전부 롤백된다.
     */
    @Transactional
    public void updateActiveGeneration(Integer generation) {
        activeGenerationPutService.updateActiveGeneration(generation);
        eventPublisher.publishEvent(new ActiveGenerationChangedEvent(generation));
    }
}
