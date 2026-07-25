package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.event.SocialAccountIntegratedEvent;
import com.tavemakers.surf.domain.member.exception.IntegrationNotEligibleException;
import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationExpiredException;
import com.tavemakers.surf.domain.member.exception.PendingIntegrationNotFoundException;
import com.tavemakers.surf.domain.member.exception.ProviderAlreadyLinkedException;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.member.repository.SocialAccountRepository;
import com.tavemakers.surf.presentation.member.dto.response.SocialAccountIntegrateResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/** 소셜 계정 통합(연동) 검증·확정 로직 (§3.6). */
@ExtendWith(MockitoExtension.class)
class SocialAccountIntegrateUsecaseTest {

    @Mock private PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CareerRepository careerRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private MemberGetService memberGetService;
    @Mock private MemberBlacklistGetService memberBlacklistGetService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ApplicationContext context;

    @InjectMocks private SocialAccountIntegrateUsecase usecase;

    private static final Long EXISTING_ID = 1L;
    private static final Long TEMP_ID = 2L;
    private static final Long SA_ID = 3L;
    private static final String EMAIL = "user@test.com";
    private static final String PHONE = "01011112222";
    private static final Provider PROVIDER = Provider.APPLE;

    private Member existingMember(MemberStatus status, String email, String phone, Provider... providers) {
        Member m = Member.builder().status(status).email(email).phoneNumber(phone).build();
        ReflectionTestUtils.setField(m, "id", EXISTING_ID);
        for (Provider p : providers) {
            m.addSocialAccount(SocialAccount.builder().provider(p).providerId("pid-" + p).build());
        }
        return m;
    }

    private PendingSocialIntegration pending(LocalDateTime now) {
        return PendingSocialIntegration.issue(TEMP_ID, SA_ID, PROVIDER, EMAIL, PHONE, now);
    }

    private Member memberOf(Long id, MemberStatus status) {
        Member m = Member.builder().status(status).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Member tempMember(MemberStatus status) {
        return memberOf(TEMP_ID, status);
    }

    /** owner가 소유한 SocialAccount — getMember()가 owner를 가리키도록 연결한다. */
    private SocialAccount ownedSocialAccount(Provider provider, Member owner) {
        SocialAccount sa = SocialAccount.builder().provider(provider).providerId("sub-" + provider).build();
        owner.addSocialAccount(sa); // sa.member = owner
        return sa;
    }

    @Test
    @DisplayName("성공 — SocialAccount를 기존 회원으로 재연결하고 pending 삭제·토큰 무효화 이벤트 발행 후 provider를 반환한다")
    void integrate_success() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE); // APPLE 미보유
        Member temp = tempMember(MemberStatus.REGISTERING);
        SocialAccount socialAccount = ownedSocialAccount(PROVIDER, temp);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(temp));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(socialAccount));

        SocialAccountIntegrateResDTO result = usecase.integrateOnce(EXISTING_ID, pending.getToken());

        assertThat(result.provider()).isEqualTo("APPLE");
        assertThat(socialAccount.getMember()).isSameAs(existing);
        assertThat(existing.getSocialAccounts()).contains(socialAccount);
        then(pendingSocialIntegrationRepository).should().delete(pending);
        then(eventPublisher).should().publishEvent(new SocialAccountIntegratedEvent(TEMP_ID));
        then(memberRepository).should().delete(temp); // 통합 고아 즉시 삭제 (5.A-12 개정)
    }

    @Test
    @DisplayName("토큰 부재(사용됨/무효) — PendingIntegrationNotFoundException")
    void integrate_tokenNotFound() {
        given(pendingSocialIntegrationRepository.findByToken("bad")).willReturn(Optional.empty());

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, "bad"))
                .isInstanceOf(PendingIntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("만료 — PendingIntegrationExpiredException")
    void integrate_expired() {
        PendingSocialIntegration expired = pending(LocalDateTime.now().minusHours(1)); // expiresAt 과거
        given(pendingSocialIntegrationRepository.findByToken(expired.getToken())).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, expired.getToken()))
                .isInstanceOf(PendingIntegrationExpiredException.class);
        then(memberGetService).should(never()).getMember(EXISTING_ID);
    }

    @Test
    @DisplayName("이메일 불일치 — IntegrationNotEligibleException")
    void integrate_emailMismatch() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, "other@test.com", PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("전화번호 불일치 — IntegrationNotEligibleException")
    void integrate_phoneMismatch() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, "01099998888");
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("기존 회원이 온보딩 미완료(REGISTERING) — IntegrationNotEligibleException")
    void integrate_statusNotEligible() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.REGISTERING, EMAIL, PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("기존 회원이 이미 같은 provider 보유 — ProviderAlreadyLinkedException")
    void integrate_providerAlreadyLinked() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE, PROVIDER); // APPLE 이미 보유
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(ProviderAlreadyLinkedException.class);
        then(socialAccountRepository).should(never()).findById(SA_ID);
    }

    @Test
    @DisplayName("블랙리스트 — MemberBlacklistedException (재연결하지 않음)")
    void integrate_blacklisted() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        willThrow(new MemberBlacklistedException())
                .given(memberBlacklistGetService).validateNotBlacklisted(null, EMAIL, PHONE);

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(MemberBlacklistedException.class);
        then(socialAccountRepository).should(never()).findById(SA_ID);
        then(pendingSocialIntegrationRepository).should(never()).delete(pending);
    }

    @Test
    @DisplayName("임시 회원 행 부재 — IntegrationNotEligibleException")
    void integrate_tempMemberMissing() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("임시 회원이 발급 후 온보딩을 완료(REGISTERING 아님)했으면 IntegrationNotEligibleException — 로그인 수단 탈취 방지")
    void integrate_tempMemberAlreadyOnboarded() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(tempMember(MemberStatus.WAITING)));

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
        then(pendingSocialIntegrationRepository).should(never()).delete(pending);
    }

    @Test
    @DisplayName("대상 SocialAccount 부재(이미 이전됨) — IntegrationNotEligibleException")
    void integrate_socialAccountMissing() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(tempMember(MemberStatus.REGISTERING)));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("대상 SocialAccount가 임시 회원 소유가 아니면(이미 이전) IntegrationNotEligibleException — 재연결하지 않음")
    void integrate_socialAccountNotOwnedByTemp() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        SocialAccount notOwned = ownedSocialAccount(PROVIDER, memberOf(999L, MemberStatus.REGISTERING)); // 다른 회원 소유
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(tempMember(MemberStatus.REGISTERING)));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(notOwned));

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
        then(pendingSocialIntegrationRepository).should(never()).delete(pending);
    }

    @Test
    @DisplayName("대상 SocialAccount의 provider가 pending과 다르면 IntegrationNotEligibleException")
    void integrate_socialAccountProviderMismatch() {
        PendingSocialIntegration pending = pending(LocalDateTime.now()); // pending.provider = APPLE
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        Member temp = tempMember(MemberStatus.REGISTERING);
        SocialAccount mismatched = ownedSocialAccount(Provider.KAKAO, temp); // provider 불일치
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(temp));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(mismatched));

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("동일 provider 동시 통합 경쟁 — flush의 UNIQUE 위반을 ProviderAlreadyLinkedException(409)으로 변환하고 pending을 삭제하지 않는다")
    void integrate_sameProviderRace_translatesToProviderAlreadyLinked() {
        PendingSocialIntegration pending = pending(LocalDateTime.now());
        Member existing = existingMember(MemberStatus.WAITING, EMAIL, PHONE);
        Member temp = tempMember(MemberStatus.REGISTERING);
        SocialAccount socialAccount = ownedSocialAccount(PROVIDER, temp);
        given(pendingSocialIntegrationRepository.findByToken(pending.getToken())).willReturn(Optional.of(pending));
        given(memberGetService.getMember(EXISTING_ID)).willReturn(existing);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(temp));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(socialAccount));
        willThrow(new DataIntegrityViolationException("uk_social_member_provider"))
                .given(socialAccountRepository).flush();

        assertThatThrownBy(() -> usecase.integrateOnce(EXISTING_ID, pending.getToken()))
                .isInstanceOf(ProviderAlreadyLinkedException.class);
        then(pendingSocialIntegrationRepository).should(never()).delete(pending);
        then(eventPublisher).shouldHaveNoInteractions();
        then(memberRepository).should(never()).delete(temp);
    }

    @Test
    @DisplayName("발급 부재 경로와의 lock 교차로 인한 PessimisticLockingFailureException은 bounded 재시도로 흡수한다")
    void integrate_retriesOnPessimisticLockFailure() {
        SocialAccountIntegrateUsecase self = mock(SocialAccountIntegrateUsecase.class);
        given(context.getBean(SocialAccountIntegrateUsecase.class)).willReturn(self);
        SocialAccountIntegrateResDTO ok = new SocialAccountIntegrateResDTO("APPLE");
        given(self.integrateOnce(EXISTING_ID, "tok"))
                .willThrow(new CannotAcquireLockException("deadlock")) // 1회차: transient 데드락
                .willReturn(ok);                                        // 2회차: 성공

        SocialAccountIntegrateResDTO result = usecase.integrate(EXISTING_ID, "tok");

        assertThat(result).isSameAs(ok);
        then(self).should(times(2)).integrateOnce(EXISTING_ID, "tok");
    }

    @Test
    @DisplayName("재시도 상한을 소진하면 마지막 lock 예외를 전파한다")
    void integrate_exhaustsRetriesThenThrows() {
        SocialAccountIntegrateUsecase self = mock(SocialAccountIntegrateUsecase.class);
        given(context.getBean(SocialAccountIntegrateUsecase.class)).willReturn(self);
        given(self.integrateOnce(EXISTING_ID, "tok"))
                .willThrow(new CannotAcquireLockException("deadlock"));

        assertThatThrownBy(() -> usecase.integrate(EXISTING_ID, "tok"))
                .isInstanceOf(CannotAcquireLockException.class);
        then(self).should(times(3)).integrateOnce(EXISTING_ID, "tok"); // INTEGRATE_MAX_ATTEMPTS
    }
}
