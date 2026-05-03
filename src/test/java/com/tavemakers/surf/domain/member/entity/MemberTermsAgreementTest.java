package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTermsAgreementTest {

    private Member createMember() {
        return Member.builder()
                .kakaoId(123456789L)
                .name("테스트유저")
                .email("test@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }

    @Test
    @DisplayName("회원 생성 시 약관 동의 여부는 false이다")
    void defaultTermsAgreedIsFalse() {
        // given & when
        Member member = createMember();

        // then
        assertThat(member.isTermsAgreed()).isFalse();
    }

    @Test
    @DisplayName("agreeTerms 호출 시 약관 동의 여부가 true로 변경된다")
    void agreeTermsChangesToTrue() {
        // given
        Member member = createMember();

        // when
        member.agreeTerms();

        // then
        assertThat(member.isTermsAgreed()).isTrue();
    }

    @Test
    @DisplayName("이미 동의한 회원이 다시 agreeTerms를 호출해도 true를 유지한다")
    void agreeTermsIdempotent() {
        // given
        Member member = createMember();
        member.agreeTerms();

        // when
        member.agreeTerms();

        // then
        assertThat(member.isTermsAgreed()).isTrue();
    }
}
