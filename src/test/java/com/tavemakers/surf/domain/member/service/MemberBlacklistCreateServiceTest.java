package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.MemberBlacklist;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.repository.MemberBlacklistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** 블랙리스트 등록 기준 검증 (5.A-8) — 통합 이메일/전화번호만 사용하고 provider 식별자는 쓰지 않는다. */
@ExtendWith(MockitoExtension.class)
class MemberBlacklistCreateServiceTest {

    @Mock
    private MemberBlacklistRepository memberBlacklistRepository;

    @InjectMocks
    private MemberBlacklistCreateService memberBlacklistCreateService;

    private Member member(String email, String phone) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("legacy-1")
                .name("회원")
                .email(email)
                .phoneNumber(phone)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    @DisplayName("신규면 정규화된 이메일/전화번호로 블랙리스트를 저장한다")
    void createIfAbsent_savesWithNormalizedEmailAndPhone() {
        given(memberBlacklistRepository.existsByEmail("hong@test.com")).willReturn(false);
        given(memberBlacklistRepository.existsByPhoneNumber("01012345678")).willReturn(false);

        memberBlacklistCreateService.createIfAbsent(
                member(" Hong@Test.com ", "010-1234-5678"), MemberBlacklistActionType.EXPEL, 999L);

        ArgumentCaptor<MemberBlacklist> captor = ArgumentCaptor.forClass(MemberBlacklist.class);
        then(memberBlacklistRepository).should().save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("hong@test.com");
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("이메일이 이미 등록되어 있으면 저장하지 않는다")
    void createIfAbsent_skipsWhenEmailAlreadyBlacklisted() {
        given(memberBlacklistRepository.existsByEmail("hong@test.com")).willReturn(true);

        memberBlacklistCreateService.createIfAbsent(
                member("hong@test.com", "01012345678"), MemberBlacklistActionType.DISMISS, 999L);

        then(memberBlacklistRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("전화번호가 이미 등록되어 있으면 저장하지 않는다")
    void createIfAbsent_skipsWhenPhoneAlreadyBlacklisted() {
        given(memberBlacklistRepository.existsByEmail("hong@test.com")).willReturn(false);
        given(memberBlacklistRepository.existsByPhoneNumber("01012345678")).willReturn(true);

        memberBlacklistCreateService.createIfAbsent(
                member("hong@test.com", "01012345678"), MemberBlacklistActionType.EXPEL, 999L);

        then(memberBlacklistRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("통합 이메일이 없으면(온보딩 미완료 등) 블랙리스트 생성에 실패한다")
    void createIfAbsent_failsWithoutUnifiedEmail() {
        assertThatThrownBy(() -> memberBlacklistCreateService.createIfAbsent(
                member(null, "01012345678"), MemberBlacklistActionType.EXPEL, 999L))
                .isInstanceOf(IllegalStateException.class);

        then(memberBlacklistRepository).should(never()).save(any());
    }
}
