package com.tavemakers.surf.domain.home.repository;

import com.tavemakers.surf.domain.home.entity.HomeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeContentRepository extends JpaRepository<HomeContent, Long> {
}
