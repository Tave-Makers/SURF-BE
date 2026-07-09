package com.tavemakers.surf.domain.home.repository;

import com.tavemakers.surf.domain.home.entity.HomeBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeBannerRepository extends JpaRepository<HomeBanner, Long> {
    List<HomeBanner> findAllByOrderByDisplayOrderAsc();

    @Query("select max(b.displayOrder) from HomeBanner b")
    Optional<Integer> findMaxDisplayOrder();

    List<HomeBanner> findAllByStatusTrueOrderByDisplayOrderAsc();
}