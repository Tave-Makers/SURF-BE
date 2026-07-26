package com.tavemakers.surf.application.member.query;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.IntegrationNotEligibleException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationExpiredException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationNotFoundException;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import com.tavemakers.surf.presentation.member.dto.response.IntegrationTargetResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 임시 회원의 계정 통합 대상을 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegrationTargetGetService {

    private final PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberGetService memberGetService;

    /** 임시 회원의 소셜 계정에 연결된 통합 대상을 조회한다. */
    public IntegrationTargetResDTO getIntegrationTarget(Long tempMemberId) {
        Member tempMember = memberGetService.getMember(tempMemberId);
        validateRegisteringMember(tempMember);

        SocialAccount ownedAccount = findSingleSocialAccount(tempMemberId);
        PendingSocialIntegration pending = getPendingIntegration(ownedAccount.getId());
        validatePendingContext(tempMemberId, ownedAccount, pending);

        if (pending.isExpired(LocalDateTime.now())) {
            throw new PendingIntegrationExpiredException();
        }

        // 확장 배포 중 구버전에서 생성한 대상 미기록 행은 통합할 수 없다.
        if (!pending.hasTargetMember()) {
            throw new IntegrationNotEligibleException();
        }

        Member target = memberGetService.getMember(pending.getTargetMemberId());
        List<SocialAccount> targetSocialAccounts = socialAccountRepository.findAllByMemberId(target.getId());
        validateEligibleTarget(pending, target, targetSocialAccounts);

        return IntegrationTargetResDTO.of(pending, target, targetSocialAccounts);
    }

    /** 인증 회원이 온보딩 이전 상태인지 검증한다. */
    private void validateRegisteringMember(Member tempMember) {
        if (!tempMember.isRegistering()) {
            throw new IntegrationNotEligibleException();
        }
    }

    /** 임시 회원이 단독으로 보유한 소셜 계정을 조회한다. */
    private SocialAccount findSingleSocialAccount(Long tempMemberId) {
        List<SocialAccount> accounts = socialAccountRepository.findAllByMemberId(tempMemberId);
        if (accounts.size() != 1) {
            throw new IntegrationNotEligibleException();
        }
        return accounts.get(0);
    }

    /** 소셜 계정의 통합 대기 정보를 조회한다. */
    private PendingSocialIntegration getPendingIntegration(Long socialAccountId) {
        return pendingSocialIntegrationRepository.findBySocialAccountId(socialAccountId)
                .orElseThrow(PendingIntegrationNotFoundException::new);
    }

    /** 통합 대기 정보가 인증 회원의 소셜 계정과 일치하는지 검증한다. */
    private void validatePendingContext(
            Long tempMemberId,
            SocialAccount ownedAccount,
            PendingSocialIntegration pending
    ) {
        if (!pending.getTempMemberId().equals(tempMemberId)
                || ownedAccount.getProvider() != pending.getProvider()) {
            throw new IntegrationNotEligibleException();
        }
    }

    /** 대상 회원의 상태·연락처·소셜 계정이 통합 조건을 충족하는지 검증한다. */
    private void validateEligibleTarget(
            PendingSocialIntegration pending,
            Member target,
            List<SocialAccount> targetSocialAccounts
    ) {
        MemberStatus status = target.getStatus();
        if (status != MemberStatus.WAITING && status != MemberStatus.APPROVED) {
            throw new IntegrationNotEligibleException();
        }
        if (!pending.matchesContactInfo(target.getEmail(), target.getPhoneNumber())) {
            throw new IntegrationNotEligibleException();
        }
        boolean alreadyLinked = targetSocialAccounts.stream()
                .anyMatch(socialAccount -> socialAccount.getProvider() == pending.getProvider());
        if (alreadyLinked) {
            throw new IntegrationNotEligibleException();
        }
    }
}
