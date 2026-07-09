package com.tavemakers.surf.domain.activity.repository;

import com.tavemakers.surf.domain.activity.entity.ActiveGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveGenerationRepository extends JpaRepository<ActiveGeneration, Long> {
}