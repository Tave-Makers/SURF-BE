package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberPatchServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberPatchService memberPatchService;

    private Member member(MemberRole role) {
        Member member = Member.builder()
                .name("홍길동")
                .email("old@test.com")
                .university("old대")
                .graduateSchool("old대학원")
                .phoneNumber("01000000000")
                .phoneNumberPublic(false)
                .profileImageUrl("http://old-image.png")
                .status(MemberStatus.APPROVED)
                .role(role)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    @DisplayName("null인 필드는 건드리지 않고, null이 아닌 필드만 부분 반영한다")
    void updateProfile_appliesOnlyNonNullFields() {
        Member member = member(MemberRole.MEMBER);

        memberPatchService.updateProfile(
                member,
                null, // email 유지
                "새대학교",
                null, // 대학원 유지
                "자기소개",
                null, // link 유지
                "01099998888",
                true,
                null, // 이미지 유지(플래그 false)
                false);

        assertThat(member.getEmail()).isEqualTo("old@test.com");
        assertThat(member.getUniversity()).isEqualTo("새대학교");
        assertThat(member.getGraduateSchool()).isEqualTo("old대학원");
        assertThat(member.getSelfIntroduction()).isEqualTo("자기소개");
        assertThat(member.getLink()).isNull();
        assertThat(member.getPhoneNumber()).isEqualTo("01099998888");
        assertThat(member.getPhoneNumberPublic()).isTrue();
        // isProfileImageChanged=false이므로 프로필 이미지는 그대로 유지되어야 한다.
        assertThat(member.getProfileImageUrl()).isEqualTo("http://old-image.png");
    }

    @Test
    @DisplayName("isProfileImageChanged가 true면 프로필 이미지가 새 값(null 포함)으로 교체된다")
    void updateProfile_whenImageChanged_replacesProfileImage() {
        Member member = member(MemberRole.MEMBER);

        memberPatchService.updateProfile(
                member, null, null, null, null, null, null, null, null, true);

        assertThat(member.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("역할이 주어지면 회원의 권한을 교체한다")
    void grantRole_changesRole() {
        Member member = member(MemberRole.MEMBER);

        memberPatchService.grantRole(member, MemberRole.MANAGER);

        assertThat(member.getRole()).isEqualTo(MemberRole.MANAGER);
    }

    @Test
    @DisplayName("역할이 null이면 기존 권한이 그대로 유지된다")
    void grantRole_withNullRole_keepsExistingRole() {
        Member member = member(MemberRole.MANAGER);

        memberPatchService.grantRole(member, null);

        assertThat(member.getRole()).isEqualTo(MemberRole.MANAGER);
    }

    @Test
    @DisplayName("회원을 찾을 수 없으면 예외를 던진다")
    void agreeTerms_whenMemberNotFound_throws() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberPatchService.agreeTerms(1L))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("회원을 찾으면 약관 동의 상태로 전환한다")
    void agreeTerms_whenMemberFound_setsTermsAgreed() {
        Member member = member(MemberRole.MEMBER);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberPatchService.agreeTerms(1L);

        assertThat(member.isTermsAgreed()).isTrue();
        then(memberRepository).should().findById(1L);
    }

    @Test
    @DisplayName("V2 - 여러 회원의 권한을 한 번에 동일한 역할로 교체한다")
    void grantRoleV2_changesRoleForAllMembers() {
        Member manager = member(MemberRole.MEMBER);
        Member president = member(MemberRole.MANAGER);

        memberPatchService.grantRoleV2(List.of(manager, president), MemberRole.MEMBER);

        assertThat(manager.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(president.getRole()).isEqualTo(MemberRole.MEMBER);
    }
}
