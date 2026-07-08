package com.tavemakers.surf.domain.activity.application.query;


import com.tavemakers.surf.domain.activity.presentation.dto.activeGeneration.response.ActiveGenerationMemberResDTO;
import com.tavemakers.surf.domain.activity.domain.entity.ActiveGeneration;
import com.tavemakers.surf.domain.activity.domain.exception.ActiveGenerationNotInitializedException;
import com.tavemakers.surf.domain.activity.domain.repository.ActiveGenerationRepository;
import com.tavemakers.surf.domain.member.application.query.MemberGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActiveGenerationGetService {

    private final ActiveGenerationRepository activeGenerationRepository;
    private final MemberGetService memberGetService;

    @Transactional(readOnly = true)
    public Integer getActiveGeneration() {
        return activeGenerationRepository.findById(ActiveGeneration.ID)
                .orElseThrow(ActiveGenerationNotInitializedException::new)
                .getGeneration();
    }

    @Transactional(readOnly = true)
    public List<ActiveGenerationMemberResDTO> getActiveGenerationMembers() {
        Integer activeGeneration = getActiveGeneration();

        return memberGetService.getMembersByTrackGeneration(activeGeneration).stream()
                .map(ActiveGenerationMemberResDTO::from)
                .toList();
    }
}