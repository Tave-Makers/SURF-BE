package com.tavemakers.surf.domain.member.domain.validator;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.exception.RoleChangeNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleChangeValidatorTest {

    private final RoleChangeValidator validator = new RoleChangeValidator();

    private Member memberWithRole(MemberRole role) {
        return Member.builder().role(role).build();
    }

    @Test
    @DisplayName("MANAGER는 자기 자신(또는 타인)을 ADMIN으로 승격할 수 없다 — 권한 상승 차단")
    void MANAGER는_ADMIN_승격_불가() {
        Member manager = memberWithRole(MemberRole.MANAGER);

        assertThatThrownBy(() -> validator.validate(manager, manager, MemberRole.ADMIN))
                .isInstanceOf(RoleChangeNotAllowedException.class);
    }

    @Test
    @DisplayName("MANAGER는 자신과 같은 등급(MANAGER)도 부여할 수 없다")
    void MANAGER는_동급_부여_불가() {
        Member manager = memberWithRole(MemberRole.MANAGER);
        Member member = memberWithRole(MemberRole.MEMBER);

        assertThatThrownBy(() -> validator.validate(manager, member, MemberRole.MANAGER))
                .isInstanceOf(RoleChangeNotAllowedException.class);
    }

    @Test
    @DisplayName("자신보다 높거나 같은 권한을 가진 회원의 역할은 변경할 수 없다")
    void 상급자_역할_변경_불가() {
        Member president = memberWithRole(MemberRole.PRESIDENT);
        Member admin = memberWithRole(MemberRole.ADMIN);

        assertThatThrownBy(() -> validator.validate(president, admin, MemberRole.MEMBER))
                .isInstanceOf(RoleChangeNotAllowedException.class);
    }

    @Test
    @DisplayName("ADMIN은 MEMBER에게 MANAGER 권한을 부여할 수 있다")
    void ADMIN은_하위_권한_부여_가능() {
        Member admin = memberWithRole(MemberRole.ADMIN);
        Member member = memberWithRole(MemberRole.MEMBER);

        assertThatCode(() -> validator.validate(admin, member, MemberRole.MANAGER))
                .doesNotThrowAnyException();
    }
}
