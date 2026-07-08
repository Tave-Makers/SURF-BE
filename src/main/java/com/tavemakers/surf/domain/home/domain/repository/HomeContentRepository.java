package com.tavemakers.surf.domain.home.domain.repository;

import com.tavemakers.surf.domain.home.domain.entity.HomeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeContentRepository extends JpaRepository<HomeContent, Long> {
}
