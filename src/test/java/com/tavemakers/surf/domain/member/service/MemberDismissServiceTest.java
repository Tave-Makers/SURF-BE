package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.MemberDismissNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 협력자가 없는 순수 도메인 검증 로직이므로 Mockito 없이 직접 인스턴스화한다. */
class MemberDismissServiceTest {

    private final MemberDismissService memberDismissService = new MemberDismissService();

    @Test
    @DisplayName("APPROVED 상태 회원은 제명 가능하다 (예외 없음)")
    void validateDismissible_whenApproved_doesNotThrow() {
        Member member = Member.builder().status(MemberStatus.APPROVED).build();

        assertThatCode(() -> memberDismissService.validateDismissible(member))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("APPROVED가 아닌 회원은 제명할 수 없다")
    void validateDismissible_whenNotApproved_throws() {
        Member member = Member.builder().status(MemberStatus.WAITING).build();

        assertThatThrownBy(() -> memberDismissService.validateDismissible(member))
                .isInstanceOf(MemberDismissNotAllowedException.class);
    }
}
