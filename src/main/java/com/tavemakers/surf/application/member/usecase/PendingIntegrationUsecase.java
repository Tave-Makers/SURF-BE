package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 통합 대기 row 발급 — 온보딩 case B 감지 시 호출된다.
 * 온보딩 트랜잭션은 감지 예외로 롤백되므로, pending row는 {@code REQUIRES_NEW}로 독립 커밋해야 살아남는다. (§3.6)
 */
@Service
@RequiredArgsConstructor
public class PendingIntegrationUsecase {

    private final PendingSocialIntegrationRepository pendingSocialIntegrationRepository;

    /** 온보딩 롤백과 독립적으로 통합 대기 row를 커밋하고 1회성 토큰을 반환한다. 기존 pending은 삭제 후 재발급한다(계정당 유효 토큰 1개). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String issue(Long tempMemberId, Long socialAccountId, Provider provider,
                        String normalizedEmail, String normalizedPhone) {
        pendingSocialIntegrationRepository.deleteBySocialAccountId(socialAccountId);
        // Hibernate flush는 INSERT를 DELETE보다 먼저 내보내므로, 삭제를 먼저 flush해야 재발급 INSERT가 UNIQUE에 걸리지 않는다
        pendingSocialIntegrationRepository.flush();
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                tempMemberId, socialAccountId, provider, normalizedEmail, normalizedPhone, LocalDateTime.now());
        // saveAndFlush — 동시 이중 제출의 UNIQUE 경쟁을 커밋 시점이 아닌 이 트랜잭션 안에서 노출한다
        pendingSocialIntegrationRepository.saveAndFlush(pending);
        return pending.getToken();
    }

    /** 동시 이중 제출 경쟁의 패자 복구 — 승자가 커밋한 pending 토큰을 새 트랜잭션으로 읽어 반환한다(멱등 발급). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<String> findIssuedToken(Long socialAccountId) {
        return pendingSocialIntegrationRepository.findBySocialAccountId(socialAccountId)
                .map(PendingSocialIntegration::getToken);
    }
}
