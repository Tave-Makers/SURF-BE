package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.event.TeamDeletedEvent;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotFoundException;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotInMemberException;
import com.tavemakers.surf.domain.team.exception.TeamMemberDuplicatedException;
import com.tavemakers.surf.domain.team.exception.TeamNotFoundException;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TeamService 단위 테스트 — resolveMembers(리더 포함/중복/조회 검증)와
 * createTeam/updateTeam 의 팀원 재정렬 로직을 Spring 컨텍스트 없이 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private MemberGetService memberGetService;
    @Mock
    private PersonalScoreCreateService personalScoreCreateService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository, memberGetService, personalScoreCreateService, eventPublisher);
    }

    private Member memberWithId(Long id, String name) {
        Member member = Member.builder().name(name).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("createTeam: 팀원 ID 목록에 중복이 있으면 TeamMemberDuplicatedException 발생, 멤버 조회는 호출되지 않는다")
    void createTeam_중복된_팀원ID_예외() {
        List<Long> memberIds = List.of(1L, 2L, 2L);

        assertThatThrownBy(() -> teamService.createTeam(9, TeamType.STUDY, "팀", "설명", 1L, memberIds))
                .isInstanceOf(TeamMemberDuplicatedException.class);

        then(memberGetService).should(never()).getMembersByIds(anyList());
        then(teamRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createTeam: 팀장이 팀원 목록에 없으면 TeamLeaderNotInMemberException 발생")
    void createTeam_팀장이_팀원목록에_없음_예외() {
        List<Long> memberIds = List.of(2L, 3L);

        assertThatThrownBy(() -> teamService.createTeam(9, TeamType.STUDY, "팀", "설명", 1L, memberIds))
                .isInstanceOf(TeamLeaderNotInMemberException.class);

        then(memberGetService).should(never()).getMembersByIds(anyList());
    }

    @Test
    @DisplayName("createTeam: 조회된 멤버 수가 요청보다 적으면 MemberNotFoundException 발생")
    void createTeam_존재하지않는_멤버_예외() {
        List<Long> memberIds = List.of(1L, 2L, 3L);
        given(memberGetService.getMembersByIds(anyList()))
                .willReturn(List.of(memberWithId(1L, "리더"), memberWithId(2L, "멤버2")));

        assertThatThrownBy(() -> teamService.createTeam(9, TeamType.STUDY, "팀", "설명", 1L, memberIds))
                .isInstanceOf(MemberNotFoundException.class);

        then(teamRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createTeam: 조회된 멤버 목록에 리더 ID가 실제로 없으면 TeamLeaderNotFoundException 발생")
    void createTeam_리더가_조회결과에_없음_예외() {
        List<Long> memberIds = List.of(1L, 2L);
        // 요청 개수(2)와 동일하지만 리더(1L)를 포함하지 않는 방어적 케이스
        given(memberGetService.getMembersByIds(anyList()))
                .willReturn(List.of(memberWithId(2L, "멤버2"), memberWithId(3L, "멤버3")));

        assertThatThrownBy(() -> teamService.createTeam(9, TeamType.STUDY, "팀", "설명", 1L, memberIds))
                .isInstanceOf(TeamLeaderNotFoundException.class);
    }

    @Test
    @DisplayName("createTeam: 정상 입력이면 리더를 포함한 팀이 생성되고 팀 점수가 저장된다")
    void createTeam_정상생성() {
        List<Long> memberIds = List.of(1L, 2L, 3L);
        Member leader = memberWithId(1L, "리더");
        Member m2 = memberWithId(2L, "멤버2");
        Member m3 = memberWithId(3L, "멤버3");
        given(memberGetService.getMembersByIds(anyList())).willReturn(List.of(leader, m2, m3));
        given(teamRepository.save(any(Team.class))).willAnswer(inv -> inv.getArgument(0));

        Team result = teamService.createTeam(9, TeamType.STUDY, "팀명", "설명", 1L, memberIds);

        assertThat(result.getLeader().getId()).isEqualTo(1L);
        assertThat(result.getMemberCount()).isEqualTo(3);
        Set<Long> resultMemberIds = result.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .collect(Collectors.toSet());
        assertThat(resultMemberIds).containsExactlyInAnyOrder(1L, 2L, 3L);

        then(personalScoreCreateService).should().saveTeamScore(result);
    }

    @Test
    @DisplayName("updateTeam: 존재하지 않는 팀이면 TeamNotFoundException 발생")
    void updateTeam_팀없음_예외() {
        given(teamRepository.findDetailBaseById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.updateTeam(99L, 9, TeamType.STUDY, "팀", "설명", 1L, List.of(1L)))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    @DisplayName("updateTeam: 요청 멤버ID 집합과 다르게 팀원을 추가/삭제하여 재정렬한다")
    void updateTeam_팀원_재정렬() {
        // 기존 팀: 리더(1) + 멤버2(2) + 멤버3(3)
        Member leader = memberWithId(1L, "리더");
        Member m2 = memberWithId(2L, "멤버2");
        Member m3 = memberWithId(3L, "멤버3");
        Team team = Team.of(9, TeamType.STUDY, "기존팀", "기존설명", leader);
        team.addMember(m2);
        team.addMember(m3);
        given(teamRepository.findDetailBaseById(10L)).willReturn(Optional.of(team));

        // 변경 요청: 리더(1) 유지, 멤버3(3) 제거, 멤버4(4) 신규 추가
        Member m4 = memberWithId(4L, "멤버4");
        given(memberGetService.getMembersByIds(anyList())).willReturn(List.of(leader, m2, m4));

        Team result = teamService.updateTeam(10L, 10, TeamType.PROJECT, "새팀명", "새설명", 1L, List.of(1L, 2L, 4L));

        assertThat(result.getGeneration()).isEqualTo(10);
        assertThat(result.getType()).isEqualTo(TeamType.PROJECT);
        assertThat(result.getName()).isEqualTo("새팀명");
        Set<Long> resultMemberIds = result.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .collect(Collectors.toSet());
        assertThat(resultMemberIds).containsExactlyInAnyOrder(1L, 2L, 4L);
        assertThat(resultMemberIds).doesNotContain(3L);
    }

    @Test
    @DisplayName("updateTeam: 팀장을 변경하면 새 팀장이 팀원 목록에 없어도 자동으로 포함된다")
    void updateTeam_팀장_변경() {
        Member oldLeader = memberWithId(1L, "기존리더");
        Team team = Team.of(9, TeamType.STUDY, "기존팀", "기존설명", oldLeader);
        given(teamRepository.findDetailBaseById(10L)).willReturn(Optional.of(team));

        Member newLeader = memberWithId(2L, "새리더");
        given(memberGetService.getMembersByIds(anyList())).willReturn(List.of(oldLeader, newLeader));

        Team result = teamService.updateTeam(10L, 9, TeamType.STUDY, "팀", "설명", 2L, List.of(1L, 2L));

        assertThat(result.getLeader().getId()).isEqualTo(2L);
        Set<Long> resultMemberIds = result.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .collect(Collectors.toSet());
        assertThat(resultMemberIds).contains(1L, 2L);
    }

    @Test
    @DisplayName("deleteTeam: 팀 삭제 전 TeamDeletedEvent 를 발행하고 repository.delete 를 호출한다")
    void deleteTeam_이벤트발행후_삭제() {
        Member leader = memberWithId(1L, "리더");
        Team team = Team.of(9, TeamType.STUDY, "팀", "설명", leader);
        ReflectionTestUtils.setField(team, "id", 5L);
        given(teamRepository.findById(5L)).willReturn(Optional.of(team));

        teamService.deleteTeam(5L);

        then(eventPublisher).should().publishEvent(any(TeamDeletedEvent.class));
        then(teamRepository).should().delete(team);
    }

    @Test
    @DisplayName("deleteTeam: 존재하지 않는 팀이면 TeamNotFoundException 발생")
    void deleteTeam_팀없음_예외() {
        given(teamRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(99L))
                .isInstanceOf(TeamNotFoundException.class);

        then(eventPublisher).should(never()).publishEvent(any());
        then(teamRepository).should(never()).delete(any());
    }
}
