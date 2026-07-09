package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberGenerationStatusSyncTest {

    private Member createMember(MemberType memberType, boolean activityStatus) {
        return Member.builder()
                .kakaoId(987654321L)
                .name("기수동기화테스트")
                .email("generation@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(memberType)
                .activityStatus(activityStatus)
                .build();
    }

    @Test
    @DisplayName("정책에 맞는 memberType과 활동 상태로 동기화된다")
    void syncGenerationStatusToYbAndActive() {
        Member member = createMember(MemberType.OB, false);

        member.syncGenerationStatus(MemberType.YB, true);

        assertThat(member.isYB()).isTrue();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("OB이면서 비활동 상태로도 동기화할 수 있다")
    void syncGenerationStatusToObAndInactive() {
        Member member = createMember(MemberType.YB, true);

        member.syncGenerationStatus(MemberType.OB, false);

        assertThat(member.isYB()).isFalse();
        assertThat(member.isActive()).isFalse();
    }
}
