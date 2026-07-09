package com.tavemakers.surf.domain.member.validator;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.exception.RoleChangeNotAllowedException;
import org.springframework.stereotype.Component;

@Component
public class RoleChangeValidator {

    /**
     * 역할 변경 위계 검증 — 요청자는 자신보다 낮은 권한만 부여할 수 있고,
     * 자신보다 높거나 같은 권한을 가진 회원의 역할은 변경할 수 없다.
     * (MemberRole ordinal: ADMIN(0) > PRESIDENT(1) > MANAGER(2) > MEMBER(3))
     * 이 검증이 없으면 MANAGER가 자기 자신을 ADMIN으로 승격할 수 있다.
     */
    public void validate(Member actor, Member target, MemberRole newRole) {
        MemberRole actorRole = actor.getRole();
        boolean actorOutranksNewRole = actorRole.ordinal() < newRole.ordinal();
        boolean actorOutranksTarget = actorRole.ordinal() < target.getRole().ordinal();

        if (!actorOutranksNewRole || !actorOutranksTarget) {
            throw new RoleChangeNotAllowedException();
        }
    }
}
