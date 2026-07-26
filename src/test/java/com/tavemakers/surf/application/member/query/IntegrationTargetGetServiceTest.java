package com.tavemakers.surf.application.member.query;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** 계정 통합 대상 확인 (§3.6.2) — 조회 판정은 통합 API(POST)와 동일해야 한다. */
@ExtendWith(MockitoExtension.class)
class IntegrationTargetGetServiceTest {

    @Mock private PendingSocialIntegrationRepository pendingSocialIntegrationRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private MemberGetService memberGetService;

    @InjectMocks private IntegrationTargetGetService service;

    private static final Long TEMP_ID = 1L;
    private static final Long SA_ID = 2L;
    private static final Long TARGET_ID = 3L;
    private static final String EMAIL = "hong@test.com";
    private static final String PHONE = "01011112222";
    /** 임시 회원이 새로 들고 온 provider — 대상은 이 provider 를 아직 보유하지 않아야 한다. */
    private static final Provider PROVIDER = Provider.APPLE;

    private Member memberOf(Long id, MemberStatus status) {
        Member m = Member.builder().status(status).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Member targetOf(MemberStatus status, String email, String phone) {
        Member m = Member.builder().status(status).name("홍길동").email(email).phoneNumber(phone).build();
        ReflectionTestUtils.setField(m, "id", TARGET_ID);
        ReflectionTestUtils.setField(m, "profileImageUrl", "https://cdn/profile.jpg");
        return m;
    }

    private SocialAccount socialAccountOf(Long id, Provider provider) {
        SocialAccount sa = SocialAccount.builder().provider(provider).providerId("pid-" + provider).build();
        ReflectionTestUtils.setField(sa, "id", id);
        return sa;
    }

    private PendingSocialIntegration pending(LocalDateTime now) {
        return PendingSocialIntegration.issue(TEMP_ID, SA_ID, TARGET_ID, PROVIDER, EMAIL, PHONE, now);
    }

    /** 임시 회원이 REGISTERING 이고 소셜 계정 1개를 보유한 상태. */
    private void givenTempMemberWithSingleAccount() {
        given(memberGetService.getMember(TEMP_ID)).willReturn(memberOf(TEMP_ID, MemberStatus.REGISTERING));
        given(socialAccountRepository.findAllByMemberId(TEMP_ID))
                .willReturn(List.of(socialAccountOf(SA_ID, PROVIDER)));
    }

    /** 대상 검증 직전까지의 스텁 — 임시 회원·소셜 계정·유효 pending. */
    private void givenEligibleUntilTarget() {
        givenTempMemberWithSingleAccount();
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID))
                .willReturn(Optional.of(pending(LocalDateTime.now())));
    }

    /** 정상 조회 전체 스텁 — 대상은 KAKAO 만 보유(임시 회원의 APPLE 미보유). */
    private void givenEligible(Member target) {
        givenEligibleUntilTarget();
        given(memberGetService.getMember(TARGET_ID)).willReturn(target);
        given(socialAccountRepository.findAllByMemberId(TARGET_ID))
                .willReturn(List.of(socialAccountOf(10L, Provider.KAKAO)));
    }

    @Test
    @DisplayName("성공 — 대상 프로필과 연결된 provider 를 반환한다")
    void getTarget_success() {
        givenEligible(targetOf(MemberStatus.APPROVED, EMAIL, PHONE));

        IntegrationTargetResDTO result = service.getIntegrationTarget(TEMP_ID);

        assertThat(result.providers()).containsExactly("KAKAO"); // 임시 회원의 APPLE 이 섞이지 않는다
        assertThat(result.username()).isEqualTo("홍길동");
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn/profile.jpg");
    }

    @Test
    @DisplayName("승인 대기(WAITING) 대상도 조회할 수 있다 — 통합 대상 범위와 일치")
    void getTarget_waitingTarget() {
        givenEligible(targetOf(MemberStatus.WAITING, EMAIL, PHONE));

        assertThat(service.getIntegrationTarget(TEMP_ID).username()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("이메일·전화번호는 대상 회원이 아니라 pending 값을 반환한다 — 호출자가 입력한 값의 에코")
    void getTarget_contactComesFromPending() {
        givenEligible(targetOf(MemberStatus.APPROVED, EMAIL, PHONE));

        IntegrationTargetResDTO result = service.getIntegrationTarget(TEMP_ID);

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.phoneNumber()).isEqualTo(PHONE);
    }

    @Test
    @DisplayName("조회는 부수효과가 없다 — pending 을 삭제·저장하지 않는다")
    void getTarget_hasNoSideEffect() {
        givenEligible(targetOf(MemberStatus.APPROVED, EMAIL, PHONE));

        service.getIntegrationTarget(TEMP_ID);

        then(pendingSocialIntegrationRepository).should(never()).delete(org.mockito.ArgumentMatchers.any());
        then(pendingSocialIntegrationRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
        then(pendingSocialIntegrationRepository).should(never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        // 쓰기 락 조회를 재사용하지 않는다 — 통합·발급 경로와 경합하면 안 된다
        then(pendingSocialIntegrationRepository).should(never()).findByToken(org.mockito.ArgumentMatchers.anyString());
        then(pendingSocialIntegrationRepository).should(never())
                .findBySocialAccountIdForUpdate(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("인증 주체가 REGISTERING 이 아니면 IntegrationNotEligibleException — pending 을 조회하지 않는다")
    void getTarget_tempMemberNotRegistering() {
        given(memberGetService.getMember(TEMP_ID)).willReturn(memberOf(TEMP_ID, MemberStatus.WAITING));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("임시 회원의 소셜 계정이 1개가 아니면 IntegrationNotEligibleException — 이전할 로그인 수단을 특정할 수 없다")
    void getTarget_multipleSocialAccounts() {
        given(memberGetService.getMember(TEMP_ID)).willReturn(memberOf(TEMP_ID, MemberStatus.REGISTERING));
        given(socialAccountRepository.findAllByMemberId(TEMP_ID)).willReturn(
                List.of(socialAccountOf(SA_ID, PROVIDER), socialAccountOf(99L, Provider.KAKAO)));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("보유한 소셜 계정에 대기 건이 없으면 PendingIntegrationNotFoundException (이미 사용됨/무효)")
    void getTarget_pendingNotFound() {
        givenTempMemberWithSingleAccount();
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(PendingIntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("pending 의 임시 회원이 인증 주체와 다르면 IntegrationNotEligibleException")
    void getTarget_tempMemberMismatch() {
        givenTempMemberWithSingleAccount();
        PendingSocialIntegration other = PendingSocialIntegration.issue(
                999L, SA_ID, TARGET_ID, PROVIDER, EMAIL, PHONE, LocalDateTime.now());
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("보유한 소셜 계정의 provider 가 pending 과 다르면 IntegrationNotEligibleException — 통합 API 판정과 일치시킨다")
    void getTarget_providerMismatch() {
        given(memberGetService.getMember(TEMP_ID)).willReturn(memberOf(TEMP_ID, MemberStatus.REGISTERING));
        given(socialAccountRepository.findAllByMemberId(TEMP_ID))
                .willReturn(List.of(socialAccountOf(SA_ID, Provider.KAKAO))); // pending 은 APPLE
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID))
                .willReturn(Optional.of(pending(LocalDateTime.now())));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("만료된 pending — PendingIntegrationExpiredException")
    void getTarget_expired() {
        givenTempMemberWithSingleAccount();
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID))
                .willReturn(Optional.of(pending(LocalDateTime.now().minusHours(1))));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(PendingIntegrationExpiredException.class);
    }

    @Test
    @DisplayName("대상이 기록되지 않은 구버전 pending — NPE 대신 IntegrationNotEligibleException")
    void getTarget_legacyPendingWithoutTarget() {
        // 컬럼 추가(expand) 후 구버전 인스턴스가 만든 행 — 롤링 배포 공존 구간에서 실제로 발생할 수 있다
        PendingSocialIntegration legacy = pending(LocalDateTime.now());
        ReflectionTestUtils.setField(legacy, "targetMemberId", null);
        givenTempMemberWithSingleAccount();
        given(pendingSocialIntegrationRepository.findBySocialAccountId(SA_ID)).willReturn(Optional.of(legacy));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("대상이 WAITING·APPROVED 가 아니면 IntegrationNotEligibleException")
    void getTarget_targetStatusNotEligible() {
        givenEligibleUntilTarget();
        given(memberGetService.getMember(TARGET_ID)).willReturn(targetOf(MemberStatus.REJECTED, EMAIL, PHONE));
        given(socialAccountRepository.findAllByMemberId(TARGET_ID))
                .willReturn(List.of(socialAccountOf(10L, Provider.KAKAO)));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("대상이 연락처를 바꿔 pending 값과 달라지면 IntegrationNotEligibleException — 조회 통과 후 POST 409 를 막는다")
    void getTarget_targetContactChanged() {
        givenEligibleUntilTarget();
        given(memberGetService.getMember(TARGET_ID)).willReturn(targetOf(MemberStatus.APPROVED, "changed@test.com", PHONE));
        given(socialAccountRepository.findAllByMemberId(TARGET_ID))
                .willReturn(List.of(socialAccountOf(10L, Provider.KAKAO)));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }

    @Test
    @DisplayName("대상이 이미 같은 provider 를 보유하면 IntegrationNotEligibleException (1 provider = 1 account)")
    void getTarget_targetAlreadyLinked() {
        givenEligibleUntilTarget();
        given(memberGetService.getMember(TARGET_ID)).willReturn(targetOf(MemberStatus.APPROVED, EMAIL, PHONE));
        given(socialAccountRepository.findAllByMemberId(TARGET_ID))
                .willReturn(List.of(socialAccountOf(10L, Provider.KAKAO), socialAccountOf(11L, PROVIDER)));

        assertThatThrownBy(() -> service.getIntegrationTarget(TEMP_ID))
                .isInstanceOf(IntegrationNotEligibleException.class);
    }
}
