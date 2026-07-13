package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.event.TeamDeletedEvent;
import com.tavemakers.surf.domain.team.repository.TeamMemberRepository;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TeamMemberCleanupService.cleanupOnDismiss 단위 테스트 — 리더 위임/팀 삭제 분기와
 * 팀원 제거 로직을 Spring 컨텍스트 없이 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TeamMemberCleanupServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TeamMemberCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new TeamMemberCleanupService(teamRepository, teamMemberRepository, eventPublisher);
    }

    private Member memberWithId(Long id, String name) {
        Member member = Member.builder().name(name).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Set<Long> memberIdsOf(Team team) {
        return team.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("제명 대상이 팀장이고 다른 팀원이 남아있으면 리더를 위임하고 본인만 제거한다 (팀은 삭제되지 않는다)")
    void 리더_제명시_다른팀원있으면_리더위임() {
        Member leader = memberWithId(1L, "리더");
        Member m2 = memberWithId(2L, "멤버2");
        Member m3 = memberWithId(3L, "멤버3");
        Team team = Team.of(9, TeamType.STUDY, "팀", "설명", leader);
        team.addMember(m2);
        team.addMember(m3);
        given(teamRepository.findAllByMemberIdForDismissal(1L)).willReturn(List.of(team));

        cleanupService.cleanupOnDismiss(leader);

        assertThat(team.getLeader().getId()).isEqualTo(2L);
        assertThat(memberIdsOf(team)).containsExactlyInAnyOrder(2L, 3L);
        then(teamRepository).should(never()).delete(any());
        then(eventPublisher).should(never()).publishEvent(any());
        then(teamMemberRepository).should().deleteAllByMemberId(1L);
    }

    @Test
    @DisplayName("제명 대상이 팀장이고 다른 팀원이 없으면 TeamDeletedEvent 발행 후 팀 자체를 삭제한다")
    void 리더_제명시_다른팀원없으면_팀삭제() {
        Member leader = memberWithId(1L, "리더");
        Team team = Team.of(9, TeamType.STUDY, "팀", "설명", leader);
        ReflectionTestUtils.setField(team, "id", 100L);
        given(teamRepository.findAllByMemberIdForDismissal(1L)).willReturn(List.of(team));

        cleanupService.cleanupOnDismiss(leader);

        then(eventPublisher).should().publishEvent(new TeamDeletedEvent(100L));
        then(teamRepository).should().delete(team);
        then(teamMemberRepository).should().deleteAllByMemberId(1L);
    }

    @Test
    @DisplayName("제명 대상이 팀장이 아니면 리더 변경 없이 본인만 팀원 목록에서 제거된다")
    void 팀원_제명시_본인만_제거() {
        Member leader = memberWithId(1L, "리더");
        Member target = memberWithId(3L, "제명대상");
        Team team = Team.of(9, TeamType.STUDY, "팀", "설명", leader);
        team.addMember(target);
        given(teamRepository.findAllByMemberIdForDismissal(3L)).willReturn(List.of(team));

        cleanupService.cleanupOnDismiss(target);

        assertThat(team.getLeader().getId()).isEqualTo(1L);
        assertThat(memberIdsOf(team)).containsExactly(1L);
        then(teamRepository).should(never()).delete(any());
        then(eventPublisher).should(never()).publishEvent(any());
        then(teamMemberRepository).should().deleteAllByMemberId(3L);
    }
}
