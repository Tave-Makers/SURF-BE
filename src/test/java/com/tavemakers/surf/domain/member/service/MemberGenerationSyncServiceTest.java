package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberGenerationSyncServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberGenerationSyncService memberGenerationSyncService;

    private Member createApprovedMember(String email) {
        return Member.builder()
                .kakaoId(System.nanoTime())
                .name("승인회원")
                .email(email)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }

    @Test
    @DisplayName("첫 활동 기수가 현재 활동 기수와 같으면 YB이면서 활동중으로 동기화된다")
    void syncApprovedMemberToYbAndActive() {
        Member member = createApprovedMember("yb@test.com");
        member.addTrack(17, Part.DEEP_LEARNING);

        memberGenerationSyncService.syncApprovedMember(member, 17);

        assertThat(member.isYB()).isTrue();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("이전 기수 출신이 현재 활동 기수에도 참여하면 OB이면서 활동중으로 동기화된다")
    void syncApprovedMemberToObButActive() {
        Member member = createApprovedMember("ob@test.com");
        member.addTrack(15, Part.BACKEND);
        member.addTrack(17, Part.DEEP_LEARNING);

        memberGenerationSyncService.syncApprovedMember(member, 17);

        assertThat(member.isYB()).isFalse();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("현재 활동 기수 트랙이 없으면 OB와 비활동으로 동기화된다")
    void syncApprovedMemberToObAndInactive() {
        Member member = createApprovedMember("inactive@test.com");
        member.addTrack(15, Part.BACKEND);
        member.addTrack(16, Part.DEEP_LEARNING);

        memberGenerationSyncService.syncApprovedMember(member, 17);

        assertThat(member.isYB()).isFalse();
        assertThat(member.isActive()).isFalse();
    }

    @Test
    @DisplayName("활동 기수 변경 시 승인 회원 전체를 현재 기수 기준으로 동기화한다")
    void syncApprovedMembersByGeneration() {
        Member ybMember = createApprovedMember("sync-yb@test.com");
        ybMember.addTrack(17, Part.BACKEND);

        Member obActiveMember = createApprovedMember("sync-ob-active@test.com");
        obActiveMember.addTrack(16, Part.WEB_FRONTEND);
        obActiveMember.addTrack(17, Part.DEEP_LEARNING);

        Member obInactiveMember = createApprovedMember("sync-ob-inactive@test.com");
        obInactiveMember.addTrack(16, Part.WEB_FRONTEND);

        given(memberRepository.findAllApprovedWithTracks())
                .willReturn(List.of(ybMember, obActiveMember, obInactiveMember));

        memberGenerationSyncService.syncApprovedMembersByGeneration(17);

        assertThat(ybMember.isYB()).isTrue();
        assertThat(ybMember.isActive()).isTrue();
        assertThat(obActiveMember.isYB()).isFalse();
        assertThat(obActiveMember.isActive()).isTrue();
        assertThat(obInactiveMember.isYB()).isFalse();
        assertThat(obInactiveMember.isActive()).isFalse();
    }
}
