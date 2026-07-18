package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.validator.OnboardingAccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberBlacklistGetService memberBlacklistGetService;

    @Mock
    private OnboardingAccountValidator onboardingAccountValidator;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member registeringMember() {
        return Member.builder()
                .status(MemberStatus.REGISTERING)
                .build();
    }

    @Test
    @DisplayName("가입 폼 값을 정규화(이메일 소문자·trim, 전화번호 숫자만)해 반영하고 REGISTERING 상태를 WAITING으로 전이한다")
    void signup_normalizesAndAppliesFormValues() {
        Member member = registeringMember();

        Member result = memberService.signup(
                member, "홍길동", "서울대", "서울대학원", " TEST@Example.COM ", "010-1234-5678");

        then(memberBlacklistGetService).should()
                .validateNotBlacklisted(null, "test@example.com", "01012345678");

        assertThat(result).isSameAs(member);
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getUniversity()).isEqualTo("서울대");
        assertThat(result.getGraduateSchool()).isEqualTo("서울대학원");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(result.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(result.getMemberType()).isEqualTo(MemberType.YB);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getStatus()).isEqualTo(MemberStatus.WAITING);
    }

    @Test
    @DisplayName("전화번호가 없으면 정규화된 phone은 null로 블랙리스트 검증에 전달되고 회원의 전화번호도 null로 유지된다")
    void signup_withNullPhone_keepsPhoneNull() {
        Member member = registeringMember();

        memberService.signup(member, "홍길동", "서울대", "서울대학원", "test@example.com", null);

        then(memberBlacklistGetService).should()
                .validateNotBlacklisted(null, "test@example.com", null);
        assertThat(member.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("블랙리스트 회원이면 예외를 던지고, 가입 폼 값은 회원에 전혀 반영되지 않는다")
    void signup_whenBlacklisted_throwsAndDoesNotMutateMember() {
        Member member = registeringMember();
        willThrow(new MemberBlacklistedException())
                .given(memberBlacklistGetService)
                .validateNotBlacklisted(null, "blacklisted@test.com", "01012345678");

        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", "blacklisted@test.com", "010-1234-5678"))
                .isInstanceOf(MemberBlacklistedException.class);

        assertThat(member.getName()).isNull();
        assertThat(member.getEmail()).isNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.REGISTERING);
    }
}
