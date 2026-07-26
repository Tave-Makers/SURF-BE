package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.IntegrationNotEligibleException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** 온보딩 트랜잭션과 분리해 계정 통합 대기 정보를 발급한다. */
@Service
@RequiredArgsConstructor
public class PendingIntegrationUsecase {

    private final PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

    /** 동일한 발급 요청은 기존 토큰을 반환하고 변경된 요청은 토큰을 재발급한다. */
    // READ_COMMITTED: 부재 pending 락 조회의 gap lock 데드락 회피
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public IssuedIntegrationToken issue(Long tempMemberId, Long socialAccountId, Long targetMemberId,
                                        Provider provider, String normalizedEmail, String normalizedPhone) {
        LocalDateTime now = LocalDateTime.now();

        // 기존 pending 행 쓰기 락 — integrate·동시 발급과 직렬화
        Optional<PendingSocialIntegration> existing =
                pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(socialAccountId);
        if (existing.isPresent()) {
            PendingSocialIntegration pending = existing.get();
            // 유효·동일 컨텍스트면 그대로 반환(멱등). 연락처·대상이 바뀐 재요청은 다른 명령이라 아래에서 교체한다
            if (!pending.isExpired(now)
                    && pending.matchesContext(tempMemberId, targetMemberId, provider, normalizedEmail, normalizedPhone)) {
                return new IssuedIntegrationToken(pending.getToken(), pending.getExpiresAt());
            }
            // 만료/컨텍스트 불일치 — 삭제를 먼저 flush해야 재발급 INSERT가 UNIQUE에 걸리지 않는다
            pendingSocialIntegrationRepository.delete(pending);
            pendingSocialIntegrationRepository.flush();
        }

        // INSERT 전 재검증 — integrate가 이미 소비했으면 여기서 중단(좀비 pending 방지)
        revalidateTransferable(tempMemberId, socialAccountId, provider);

        // 신규 발급 — 동시 발급의 UNIQUE 경쟁은 호출부 재시도로 수렴한다
        PendingSocialIntegration pending = PendingSocialIntegration.issue(
                tempMemberId, socialAccountId, targetMemberId, provider, normalizedEmail, normalizedPhone, now);
        pendingSocialIntegrationRepository.saveAndFlush(pending);
        return new IssuedIntegrationToken(pending.getToken(), pending.getExpiresAt());
    }

    /** 통합 대기 정보 생성 전 임시 회원과 소셜 계정의 이전 가능 여부를 검증한다. */
    private void revalidateTransferable(Long tempMemberId, Long socialAccountId, Provider provider) {
        Member tempMember = memberRepository.findWithLockingById(tempMemberId)
                .orElseThrow(IntegrationNotEligibleException::new);
        if (tempMember.getStatus() != MemberStatus.REGISTERING) {
            throw new IntegrationNotEligibleException();
        }
        SocialAccount socialAccount = socialAccountRepository.findById(socialAccountId)
                .orElseThrow(IntegrationNotEligibleException::new);
        if (!socialAccount.getMember().getId().equals(tempMemberId)
                || socialAccount.getProvider() != provider) {
            throw new IntegrationNotEligibleException();
        }
    }
}
