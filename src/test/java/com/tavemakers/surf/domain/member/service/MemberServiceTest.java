package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.validator.OnboardingAccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Test
    @DisplayName("이메일이 null/공백이면 IllegalArgumentException — DTO 검증에 의존하지 않는 서비스 자체 방어")
    void signup_blankEmail_throwsIllegalArgument() {
        Member member = registeringMember();

        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", "   ", "010-1234-5678"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", null, "010-1234-5678"))
                .isInstanceOf(IllegalArgumentException.class);

        then(memberBlacklistGetService).shouldHaveNoInteractions();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.REGISTERING);
    }

    @Test
    @DisplayName("공백/구분자만 있는 전화번호는 null로 정규화되어 검증·저장에 전달된다 (빈 문자열 조회·UNIQUE '' 점유 방지)")
    void signup_blankPhone_foldsToNull() {
        Member member = registeringMember();

        memberService.signup(member, "홍길동", "서울대", "서울대학원", "test@example.com", " -- ");

        then(memberBlacklistGetService).should()
                .validateNotBlacklisted(null, "test@example.com", null);
        then(onboardingAccountValidator).should()
                .validateForOnboarding(member, "test@example.com", null);
        assertThat(member.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("전화번호가 공백이어도(정규화 → null), 무관한 제약 위반을 PhoneAlreadyUsed로 오분류하지 않고 원 예외를 그대로 전파한다")
    void signup_blankPhone_doesNotMismapUnrelatedConstraintViolation() {
        Member member = registeringMember();
        // 정규화: phone "   " → null (빈 문자열로 남기면 String.contains("")가 항상 true → 오분류 함정)
        DataIntegrityViolationException unrelated = new DataIntegrityViolationException(
                "constraint",
                new RuntimeException("Duplicate entry 'S2024' for key 'uk_member_student_no'"));
        given(memberRepository.saveAndFlush(member)).willThrow(unrelated);

        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", "user@test.com", "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("위반 메시지에 실제 전화번호 값이 있으면 PhoneAlreadyUsedException으로 변환한다")
    void signup_realPhoneConflict_mapsToPhoneAlreadyUsed() {
        Member member = registeringMember();
        DataIntegrityViolationException conflict = new DataIntegrityViolationException(
                "constraint",
                new RuntimeException("Duplicate entry '01012345678' for key 'uk_member_phone'"));
        given(memberRepository.saveAndFlush(member)).willThrow(conflict);

        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", "user@test.com", "010-1234-5678"))
                .isInstanceOf(PhoneAlreadyUsedException.class);
    }

    @Test
    @DisplayName("위반 메시지에 실제 이메일 값이 있으면 EmailAlreadyUsedException으로 변환한다")
    void signup_realEmailConflict_mapsToEmailAlreadyUsed() {
        Member member = registeringMember();
        DataIntegrityViolationException conflict = new DataIntegrityViolationException(
                "constraint",
                new RuntimeException("Duplicate entry 'user@test.com' for key 'uk_member_email'"));
        given(memberRepository.saveAndFlush(member)).willThrow(conflict);

        assertThatThrownBy(() -> memberService.signup(
                member, "홍길동", "서울대", "서울대학원", "user@test.com", "010-1234-5678"))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }
}
