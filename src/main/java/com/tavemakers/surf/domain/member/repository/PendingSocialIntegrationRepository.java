package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingSocialIntegrationRepository extends JpaRepository<PendingSocialIntegration, Long> {

    /** 1회성 통합 토큰으로 대기 row를 행 쓰기 락 조회 — 동시 통합 요청을 직렬화해 1회성을 보장한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingSocialIntegration> findByToken(String token);

    /** SocialAccount ID로 대기 row를 행 쓰기 락 조회 — 발급을 integrate·동시 발급과 pending 행 기준으로 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingSocialIntegration p where p.socialAccountId = :socialAccountId")
    Optional<PendingSocialIntegration> findBySocialAccountIdForUpdate(@Param("socialAccountId") Long socialAccountId);
}
