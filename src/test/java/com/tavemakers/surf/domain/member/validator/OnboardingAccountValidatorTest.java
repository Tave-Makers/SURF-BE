package com.tavemakers.surf.domain.member.validator;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.AccountIntegrationAvailableException;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/** 온보딩 계정 검증 case A/B/C 판별 (§3.5). */
@ExtendWith(MockitoExtension.class)
class OnboardingAccountValidatorTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private OnboardingAccountValidator validator;

    private static final String EMAIL = "user@test.com";
    private static final String PHONE = "01011112222";

    /** id·상태·소셜 계정(provider별 1개)을 갖춘 회원 픽스처. */
    private Member member(Long id, MemberStatus status, Provider... providers) {
        Member m = Member.builder().status(status).build();
        ReflectionTestUtils.setField(m, "id", id);
        for (Provider p : providers) {
            m.addSocialAccount(SocialAccount.builder().provider(p).providerId("pid-" + p + "-" + id).build());
        }
        return m;
    }

    @Test
    @DisplayName("case A) 이메일·전화번호 둘 다 신규면 통과한다")
    void caseA_bothNew_passes() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.empty());

        assertThatCode(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("case B) 이메일·전화번호가 모두 동일한 온보딩 완료 회원 존재 + 연동 가능 provider면 통합 필요 감지")
    void caseB_fullMatch_linkable_throwsIntegration() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member owner = member(2L, MemberStatus.WAITING, Provider.APPLE); // KAKAO 미보유
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(AccountIntegrationAvailableException.class);
    }

    @Test
    @DisplayName("case C) self가 REGISTERING이 아니면(WAITING 재제출 등) 완전 일치·연동 가능이어도 통합 감지가 아니라 차단한다")
    void caseC_selfNotRegistering_throwsBlocked() {
        Member self = member(1L, MemberStatus.WAITING, Provider.KAKAO); // 이미 온보딩 완료한 self의 재제출
        Member owner = member(2L, MemberStatus.APPROVED, Provider.APPLE); // KAKAO 미보유 — self가 REGISTERING이었다면 case B
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) 이메일만 일치하면 EmailAlreadyUsedException 으로 차단한다")
    void caseC_emailOnly_throwsEmailUsed() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member owner = member(2L, MemberStatus.WAITING, Provider.APPLE);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) 전화번호만 일치하면 PhoneAlreadyUsedException 으로 차단한다")
    void caseC_phoneOnly_throwsPhoneUsed() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member owner = member(2L, MemberStatus.WAITING, Provider.APPLE);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(PhoneAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) 이메일·전화번호가 서로 다른 회원과 일치하면 차단한다(통합 아님)")
    void caseC_matchDifferentMembers_throwsBlocked() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member emailOwner = member(2L, MemberStatus.WAITING, Provider.APPLE);
        Member phoneOwner = member(3L, MemberStatus.WAITING, Provider.APPLE);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(emailOwner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(phoneOwner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) 모두 일치하나 기존 회원이 REGISTERING(온보딩 미완료)이면 통합 대상 아님 → 차단")
    void caseC_ownerRegistering_throwsBlocked() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member owner = member(2L, MemberStatus.REGISTERING, Provider.APPLE);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) 모두 일치하나 기존 회원이 이미 같은 provider 를 보유하면 1 provider=1 account 위반 → 차단")
    void caseC_ownerAlreadyHasProvider_throwsBlocked() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        Member owner = member(2L, MemberStatus.WAITING, Provider.KAKAO); // self 와 동일 provider 보유
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("case C) self 소셜 계정이 2개(비정상)면 연동 불가 → 차단")
    void caseC_selfMultipleAccounts_throwsBlocked() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO, Provider.APPLE);
        Member owner = member(2L, MemberStatus.WAITING); // provider 미보유
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("조회 결과가 self 본인이면 제외하고 신규로 취급한다(방어적 self 제외)")
    void selfExcluded_treatedAsNew_passes() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(self));
        given(memberRepository.findByPhoneNumber(PHONE)).willReturn(Optional.of(self));

        assertThatCode(() -> validator.validateForOnboarding(self, EMAIL, PHONE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("전화번호가 null 이면 전화번호 조회 없이 이메일 신규 여부만으로 통과한다")
    void nullPhone_onlyEmailChecked_passes() {
        Member self = member(1L, MemberStatus.REGISTERING, Provider.KAKAO);
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatCode(() -> validator.validateForOnboarding(self, EMAIL, null))
                .doesNotThrowAnyException();
    }
}
