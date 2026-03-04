package com.tavemakers.surf.domain.activity.service;

import com.tavemakers.surf.domain.activity.entity.ActiveGeneration;
import com.tavemakers.surf.domain.activity.exception.ActiveGenerationNotInitializedException;
import com.tavemakers.surf.domain.activity.repository.ActiveGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActiveGenerationPatchService {

    private final ActiveGenerationRepository activeGenerationRepository;

    @Transactional
    public void updateActiveGeneration(Integer generation, Long updatedBy) {
        ActiveGeneration ag = activeGenerationRepository.findById(ActiveGeneration.ID)
                .orElseThrow(ActiveGenerationNotInitializedException::new);

        ag.updateGeneration(generation);
    }
}