package com.tavemakers.surf.domain.activity.service.activeGeneration;

import com.tavemakers.surf.domain.activity.entity.ActiveGeneration;
import com.tavemakers.surf.domain.activity.exception.ActiveGenerationNotInitializedException;
import com.tavemakers.surf.domain.activity.repository.ActiveGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 활동 기수 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(ActiveGenerationUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class ActiveGenerationPutService {

    private final ActiveGenerationRepository activeGenerationRepository;

    /** 활동 기수 변경 (변경 감지로 반영) */
    public void updateActiveGeneration(Integer generation) {
        ActiveGeneration ag = activeGenerationRepository.findById(ActiveGeneration.ID)
                .orElseThrow(ActiveGenerationNotInitializedException::new);

        ag.updateGeneration(generation);
    }
}