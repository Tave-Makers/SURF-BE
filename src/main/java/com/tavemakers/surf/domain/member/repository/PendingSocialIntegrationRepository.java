package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingSocialIntegrationRepository extends JpaRepository<PendingSocialIntegration, Long> {

    /** 1회성 통합 토큰으로 대기 row를 행 쓰기 락 조회 — 동시 통합 요청을 직렬화해 1회성을 보장한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingSocialIntegration> findByToken(String token);

    /** 재발급 시 기존 pending을 제거한다 — 1 SocialAccount = 1 pending(UNIQUE) 유지. */
    void deleteBySocialAccountId(Long socialAccountId);

    Optional<PendingSocialIntegration> findBySocialAccountId(Long socialAccountId);
}
