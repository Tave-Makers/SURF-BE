package com.tavemakers.surf.domain.activity.domain.repository;

import com.tavemakers.surf.domain.activity.domain.entity.ActiveGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveGenerationRepository extends JpaRepository<ActiveGeneration, Long> {
}