package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.event.SocialAccountIntegratedEvent;
import com.tavemakers.surf.domain.member.exception.IntegrationNotEligibleException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationExpiredException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationNotFoundException;
import com.tavemakers.surf.domain.member.exception.ProviderAlreadyLinkedException;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import com.tavemakers.surf.presentation.member.dto.response.SocialAccountIntegrateResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 기존 회원에게 신규 소셜 계정을 통합한다. */
@Service
@RequiredArgsConstructor
public class SocialAccountIntegrateUsecase {

    private final PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;
    private final CareerRepository careerRepository;
    private final TrackRepository trackRepository;
    private final MemberGetService memberGetService;
    private final MemberBlacklistGetService memberBlacklistGetService;
    private final ApplicationEventPublisher eventPublisher;

    /** 통합 조건을 검증하고 소셜 계정을 기존 회원에게 이전한다. */
    @Transactional
    public SocialAccountIntegrateResDTO integrate(Long existingMemberId, String integrationToken) {
        // 행 쓰기 락으로 동시 통합 요청을 직렬화한다 — 부재는 이미 사용/무효(1회성 보장)
        PendingSocialIntegration pending = pendingSocialIntegrationRepository.findByToken(integrationToken)
                .orElseThrow(PendingIntegrationNotFoundException::new);
        if (pending.isExpired(LocalDateTime.now())) {
            throw new PendingIntegrationExpiredException();
        }

        // 감지 시점에 확정된 대상과 현재 인증 회원이 일치해야 한다.
        if (!pending.isTargetMember(existingMemberId)) {
            throw new IntegrationNotEligibleException();
        }

        // 통합 감지 후 대상 회원의 연락처가 변경되었는지 다시 확인한다.
        Member existingMember = memberGetService.getMember(existingMemberId);
        if (!pending.matchesContactInfo(existingMember.getEmail(), existingMember.getPhoneNumber())) {
            throw new IntegrationNotEligibleException();
        }
        MemberStatus status = existingMember.getStatus();
        if (status != MemberStatus.WAITING && status != MemberStatus.APPROVED) {
            throw new IntegrationNotEligibleException();
        }
        if (existingMember.hasProvider(pending.getProvider())) {
            throw new ProviderAlreadyLinkedException();
        }
        memberBlacklistGetService.validateNotBlacklisted(pending.getNormalizedEmail(), pending.getNormalizedPhone());

        // 임시 회원 행 락 — 온보딩 커밋과 직렬화해 REGISTERING을 최신 상태로 재검증한다(발급 후 온보딩 완료 회원의 로그인 수단 탈취 방지, 5.A-4)
        Member tempMember = memberRepository.findWithLockingById(pending.getTempMemberId())
                .orElseThrow(IntegrationNotEligibleException::new);
        if (tempMember.getStatus() != MemberStatus.REGISTERING) {
            throw new IntegrationNotEligibleException();
        }

        SocialAccount socialAccount = socialAccountRepository.findById(pending.getSocialAccountId())
                .orElseThrow(IntegrationNotEligibleException::new);
        if (!socialAccount.getMember().getId().equals(pending.getTempMemberId())
                || socialAccount.getProvider() != pending.getProvider()) {
            throw new IntegrationNotEligibleException();
        }

        socialAccount.reassignTo(existingMember);
        try {
            // flush로 (member_id, provider) UNIQUE 경쟁(동일 provider 동시 통합)을 이 트랜잭션 안에서 노출해 409로 변환한다
            socialAccountRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ProviderAlreadyLinkedException();
        }
        pendingSocialIntegrationRepository.delete(pending);
        eventPublisher.publishEvent(new SocialAccountIntegratedEvent(pending.getTempMemberId()));

        // 임시 회원 즉시 삭제(통합 고아 방지, 5.A-12) — 삭제 커밋으로 access token도 무력화되고,
        // 디바이스 토큰은 동기 리스너, refresh 토큰은 AFTER_COMMIT 리스너가 정리한다
        careerRepository.deleteAll(careerRepository.findByMemberId(tempMember.getId()));
        trackRepository.deleteAll(trackRepository.findByMemberId(tempMember.getId()));
        memberRepository.delete(tempMember);

        return SocialAccountIntegrateResDTO.from(pending.getProvider());
    }
}
