package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.presentation.activity.dto.activeGeneration.response.ActiveGenerationMemberResDTO;
import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationPutService;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 활동 기수 Usecase */
@Service
@RequiredArgsConstructor
public class ActiveGenerationUsecase {

    private final ActiveGenerationGetService activeGenerationGetService;
    private final ActiveGenerationPutService activeGenerationPutService;
    private final MemberGenerationSyncService memberGenerationSyncService;

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

    /** 활동 기수 변경 */
    @Transactional
    public void updateActiveGeneration(Integer generation) {
        activeGenerationPutService.updateActiveGeneration(generation);
        memberGenerationSyncService.syncApprovedMembersByGeneration(generation);
    }
}
