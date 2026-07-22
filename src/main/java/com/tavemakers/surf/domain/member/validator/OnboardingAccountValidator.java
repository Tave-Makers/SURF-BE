package com.tavemakers.surf.domain.member.validator;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.AccountIntegrationAvailableException;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 온보딩 시 통합 이메일·전화번호를 기존 회원과 대조해 case A(정상)/B(통합 필요 감지)/C(부분 일치 차단)를 판별한다. (§3.5)
 * 부분 일치는 어떤 경우에도 통과시키지 않는다. (5.A-7)
 */
@Component
@RequiredArgsConstructor
public class OnboardingAccountValidator {

    private final MemberRepository memberRepository;

    /** 온보딩 대상 회원(self, REGISTERING)을 기준으로 통합 이메일·전화번호를 검증한다. */
    public void validateForOnboarding(Member self, String normalizedEmail, String normalizedPhone) {
        Member emailOwner = findOtherOwner(memberRepository.findByEmail(normalizedEmail), self);
        Member phoneOwner = normalizedPhone == null
                ? null
                : findOtherOwner(memberRepository.findByPhoneNumber(normalizedPhone), self);

        // case A) 이메일·전화번호 둘 다 신규
        if (emailOwner == null && phoneOwner == null) {
            return;
        }

        // case B) REGISTERING self 기준, 이메일·전화번호가 모두 동일한 단일 회원이 온보딩 완료 상태 + 연동 가능 provider(정확히 1개·미보유)일 때만 통합 필요 감지 — 비REGISTERING self(WAITING 재제출 등)는 case C로 차단 (5.A-4)
        if (self.isRegistering()
                && emailOwner != null && phoneOwner != null
                && emailOwner.getId().equals(phoneOwner.getId())
                && isIntegrationTarget(emailOwner)
                && isProviderLinkable(self, emailOwner)) {
            SocialAccount socialAccount = self.getSocialAccounts().get(0); // isProviderLinkable에서 정확히 1개 보장
            throw AccountIntegrationAvailableException.detected(
                    self.getId(), socialAccount.getId(), socialAccount.getProvider(),
                    normalizedEmail, normalizedPhone);
        }

        // case C) 부분 일치 / 서로 다른 회원 / 통합 불가 상태 → 온보딩 차단
        if (emailOwner != null) {
            throw new EmailAlreadyUsedException();
        }
        throw new PhoneAlreadyUsedException();
    }

    /** 조회 결과에서 본인(self)은 제외한다. (REGISTERING self 는 email/phone 이 비어 있어 실제로는 매칭되지 않음) */
    private Member findOtherOwner(Optional<Member> found, Member self) {
        return found.filter(m -> !m.getId().equals(self.getId())).orElse(null);
    }

    /** 통합 대상은 온보딩 완료(WAITING/APPROVED) 회원으로 제한한다. (5.A-4) */
    private boolean isIntegrationTarget(Member member) {
        MemberStatus status = member.getStatus();
        return status == MemberStatus.WAITING || status == MemberStatus.APPROVED;
    }

    /** self 소셜 계정이 정확히 1개이고 기존 회원이 그 provider 를 미보유해야 연동 가능하다 (1 provider=1 account, 5.A-3). */
    private boolean isProviderLinkable(Member self, Member existing) {
        List<SocialAccount> accounts = self.getSocialAccounts();
        if (accounts.size() != 1) {
            return false;
        }
        return !existing.hasProvider(accounts.get(0).getProvider());
    }
}
