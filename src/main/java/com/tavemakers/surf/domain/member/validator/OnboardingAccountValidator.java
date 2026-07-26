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

/** 온보딩 연락처를 기존 회원과 대조해 계정 통합 가능 여부를 검증한다. */
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

        // case B) 연락처 소유자가 같고 신규 소셜 계정을 연결할 수 있으면 통합 대상으로 판정한다.
        if (self.isRegistering()
                && emailOwner != null && phoneOwner != null
                && emailOwner.getId().equals(phoneOwner.getId())
                && isIntegrationTarget(emailOwner)
                && isProviderLinkable(self, emailOwner)) {
            SocialAccount socialAccount = self.getSocialAccounts().get(0); // isProviderLinkable에서 정확히 1개 보장
            throw AccountIntegrationAvailableException.detected(
                    self.getId(), socialAccount.getId(), emailOwner.getId(), socialAccount.getProvider(),
                    normalizedEmail, normalizedPhone);
        }

        // case C) 부분 일치 / 서로 다른 회원 / 통합 불가 상태 → 온보딩 차단
        if (emailOwner != null) {
            throw new EmailAlreadyUsedException();
        }
        throw new PhoneAlreadyUsedException();
    }

    /** 연락처 조회 결과에서 온보딩 요청 회원을 제외한다. */
    private Member findOtherOwner(Optional<Member> found, Member self) {
        return found.filter(m -> !m.getId().equals(self.getId())).orElse(null);
    }

    /** 회원 상태가 계정 통합 대상에 해당하는지 확인한다. */
    private boolean isIntegrationTarget(Member member) {
        MemberStatus status = member.getStatus();
        return status == MemberStatus.WAITING || status == MemberStatus.APPROVED;
    }

    /** 임시 회원의 단일 소셜 계정을 기존 회원에게 연결할 수 있는지 확인한다. */
    private boolean isProviderLinkable(Member self, Member existing) {
        List<SocialAccount> accounts = self.getSocialAccounts();
        if (accounts.size() != 1) {
            return false;
        }
        return !existing.hasProvider(accounts.get(0).getProvider());
    }
}
