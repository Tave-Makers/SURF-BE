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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PendingIntegrationUsecaseTest {

    @Mock private PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SocialAccountRepository socialAccountRepository;

    @InjectMocks private PendingIntegrationUsecase pendingIntegrationUsecase;

    private static final Long TEMP_ID = 1L;
    private static final Long SA_ID = 2L;
    private static final Provider PROVIDER = Provider.KAKAO;
    private static final String EMAIL = "user@test.com";
    private static final String PHONE = "01011112222";

    private Member memberOf(Long id, MemberStatus status) {
        Member m = Member.builder().status(status).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    /** owner가 소유한 SocialAccount — getMember()가 owner를 가리키도록 연결한다. */
    private SocialAccount ownedSocialAccount(Provider provider, Member owner) {
        SocialAccount sa = SocialAccount.builder().provider(provider).providerId("sub-" + provider).build();
        owner.addSocialAccount(sa);
        return sa;
    }

    private void givenTransferable() {
        Member temp = memberOf(TEMP_ID, MemberStatus.REGISTERING);
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(temp));
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(ownedSocialAccount(PROVIDER, temp)));
    }

    private IssuedIntegrationToken issue() {
        return pendingIntegrationUsecase.issue(TEMP_ID, SA_ID, PROVIDER, EMAIL, PHONE);
    }

    @Test
    @DisplayName("pending 부재 — 재검증 통과 후 새 pending을 저장하고 토큰·만료를 반환한다")
    void issue_absent_savesNew() {
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.empty());
        givenTransferable();

        IssuedIntegrationToken issued = issue();

        ArgumentCaptor<PendingSocialIntegration> captor = ArgumentCaptor.forClass(PendingSocialIntegration.class);
        then(pendingSocialIntegrationRepository).should().saveAndFlush(captor.capture());
        PendingSocialIntegration saved = captor.getValue();
        assertThat(issued.token()).isEqualTo(saved.getToken());
        assertThat(issued.expiresAt()).isEqualTo(saved.getExpiresAt());
        assertThat(saved.getTempMemberId()).isEqualTo(TEMP_ID);
        assertThat(saved.getSocialAccountId()).isEqualTo(SA_ID);
        assertThat(saved.getProvider()).isEqualTo(PROVIDER);
    }

    @Test
    @DisplayName("유효한 기존 pending — 동일 토큰·만료를 그대로 반환하고 삭제·재발급하지 않는다(멱등)")
    void issue_existingValid_returnsSameToken() {
        PendingSocialIntegration valid = PendingSocialIntegration.issue(
                TEMP_ID, SA_ID, PROVIDER, EMAIL, PHONE, LocalDateTime.now());
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.of(valid));

        IssuedIntegrationToken issued = issue();

        assertThat(issued.token()).isEqualTo(valid.getToken());
        assertThat(issued.expiresAt()).isEqualTo(valid.getExpiresAt());
        then(pendingSocialIntegrationRepository).should(never()).delete(valid);
        then(pendingSocialIntegrationRepository).should(never()).saveAndFlush(any());
        then(memberRepository).should(never()).findWithLockingById(any());
    }

    @Test
    @DisplayName("만료된 기존 pending — 삭제 후 재검증하고 새 토큰을 발급한다")
    void issue_existingExpired_deletesAndReissues() {
        PendingSocialIntegration expired = PendingSocialIntegration.issue(
                TEMP_ID, SA_ID, PROVIDER, EMAIL, PHONE,
                LocalDateTime.now().minusSeconds(PendingSocialIntegration.TTL_SECONDS + 60));
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.of(expired));
        givenTransferable();

        IssuedIntegrationToken issued = issue();

        then(pendingSocialIntegrationRepository).should().delete(expired);
        then(pendingSocialIntegrationRepository).should().flush();
        ArgumentCaptor<PendingSocialIntegration> captor = ArgumentCaptor.forClass(PendingSocialIntegration.class);
        then(pendingSocialIntegrationRepository).should().saveAndFlush(captor.capture());
        assertThat(issued.token())
                .isEqualTo(captor.getValue().getToken())
                .isNotEqualTo(expired.getToken());
    }

    @Test
    @DisplayName("재검증 실패 — 임시 회원이 없거나(통합 완료) REGISTERING이 아니면 IntegrationNotEligibleException, 발급하지 않는다")
    void issue_revalidationFails_tempMemberGone() {
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.empty());
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(this::issue).isInstanceOf(IntegrationNotEligibleException.class);
        then(pendingSocialIntegrationRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("재검증 실패 — SocialAccount 소유자가 임시 회원이 아니면 IntegrationNotEligibleException")
    void issue_revalidationFails_ownershipMismatch() {
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.empty());
        given(memberRepository.findWithLockingById(TEMP_ID)).willReturn(Optional.of(memberOf(TEMP_ID, MemberStatus.REGISTERING)));
        Member otherOwner = memberOf(999L, MemberStatus.REGISTERING);
        given(socialAccountRepository.findById(SA_ID)).willReturn(Optional.of(ownedSocialAccount(PROVIDER, otherOwner)));

        assertThatThrownBy(this::issue).isInstanceOf(IntegrationNotEligibleException.class);
        then(pendingSocialIntegrationRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("유효하지만 컨텍스트가 다른 기존 pending — 연락처를 바꾼 재요청은 다른 명령이므로 삭제 후 새 토큰으로 교체한다")
    void issue_existingValid_differentContext_replaces() {
        // 같은 SocialAccount지만 이메일이 다른(온보딩 연락처를 바꿔 재제출한) 유효 pending
        PendingSocialIntegration otherContext = PendingSocialIntegration.issue(
                TEMP_ID, SA_ID, PROVIDER, "other@test.com", PHONE, LocalDateTime.now());
        given(pendingSocialIntegrationRepository.findBySocialAccountIdForUpdate(SA_ID)).willReturn(Optional.of(otherContext));
        givenTransferable();

        IssuedIntegrationToken issued = issue();

        then(pendingSocialIntegrationRepository).should().delete(otherContext);
        then(pendingSocialIntegrationRepository).should().flush();
        ArgumentCaptor<PendingSocialIntegration> captor = ArgumentCaptor.forClass(PendingSocialIntegration.class);
        then(pendingSocialIntegrationRepository).should().saveAndFlush(captor.capture());
        assertThat(issued.token())
                .isEqualTo(captor.getValue().getToken())
                .isNotEqualTo(otherContext.getToken());
        assertThat(captor.getValue().getNormalizedEmail()).isEqualTo(EMAIL); // 현재 요청의 새 컨텍스트로 발급
    }
}
