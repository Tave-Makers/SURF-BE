package com.tavemakers.surf.application.team.usecase;

import com.tavemakers.surf.application.team.query.TeamGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.service.TeamService;
import com.tavemakers.surf.presentation.team.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamGenerationSectionResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamResDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * TeamUsecase 단위 테스트 — 도메인 서비스(TeamService/TeamGetService)를 mock 하여
 * 엔티티 -> ResDTO 매핑과 요청 위임(DTO 필드 전달)만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TeamUsecaseTest {

    @Mock
    private TeamService teamService;
    @Mock
    private TeamGetService teamGetService;

    private TeamUsecase teamUsecase;

    @BeforeEach
    void setUp() {
        teamUsecase = new TeamUsecase(teamService, teamGetService);
    }

    private Member memberWithId(Long id, String name) {
        Member member = Member.builder().name(name).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("createTeam: 요청 DTO 필드를 그대로 위임하고 반환된 엔티티를 TeamResDTO 로 매핑한다")
    void createTeam_위임및매핑() {
        TeamUpsertReqDTO req = new TeamUpsertReqDTO(9, TeamType.STUDY, "팀명", "설명", 1L, List.of(1L, 2L));
        Member leader = memberWithId(1L, "리더");
        Team team = Team.of(9, TeamType.STUDY, "팀명", "설명", leader);
        team.addMember(memberWithId(2L, "멤버2"));
        ReflectionTestUtils.setField(team, "id", 10L);
        given(teamService.createTeam(9, TeamType.STUDY, "팀명", "설명", 1L, List.of(1L, 2L)))
                .willReturn(team);

        TeamResDTO result = teamUsecase.createTeam(req);

        assertThat(result.teamId()).isEqualTo(10L);
        assertThat(result.generation()).isEqualTo(9);
        assertThat(result.type()).isEqualTo(TeamType.STUDY);
        assertThat(result.name()).isEqualTo("팀명");
        assertThat(result.description()).isEqualTo("설명");
        assertThat(result.leaderId()).isEqualTo(1L);
        assertThat(result.leaderName()).isEqualTo("리더");
        assertThat(result.memberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateTeam: teamId 와 요청 DTO 필드를 그대로 위임하고 반환된 엔티티를 TeamResDTO 로 매핑한다")
    void updateTeam_위임및매핑() {
        TeamUpsertReqDTO req = new TeamUpsertReqDTO(10, TeamType.PROJECT, "새팀명", "새설명", 2L, List.of(2L));
        Member leader = memberWithId(2L, "새리더");
        Team team = Team.of(10, TeamType.PROJECT, "새팀명", "새설명", leader);
        ReflectionTestUtils.setField(team, "id", 20L);
        given(teamService.updateTeam(eq(20L), eq(10), eq(TeamType.PROJECT), eq("새팀명"), eq("새설명"), eq(2L), eq(List.of(2L))))
                .willReturn(team);

        TeamResDTO result = teamUsecase.updateTeam(20L, req);

        assertThat(result.teamId()).isEqualTo(20L);
        assertThat(result.leaderId()).isEqualTo(2L);
        assertThat(result.memberCount()).isEqualTo(1);
        then(teamService).should().updateTeam(20L, 10, TeamType.PROJECT, "새팀명", "새설명", 2L, List.of(2L));
    }

    @Test
    @DisplayName("deleteTeam: teamId 를 그대로 도메인 서비스에 위임한다")
    void deleteTeam_위임() {
        teamUsecase.deleteTeam(30L);

        then(teamService).should().deleteTeam(30L);
    }

    @Test
    @DisplayName("getTeams: 조회를 TeamGetService 에 위임하고 결과를 그대로 반환한다 (순수 위임 대표 케이스)")
    void getTeams_위임() {
        List<TeamGenerationSectionResDTO> sections = List.of(new TeamGenerationSectionResDTO(9, List.of()));
        given(teamGetService.getTeams(TeamType.STUDY)).willReturn(sections);

        List<TeamGenerationSectionResDTO> result = teamUsecase.getTeams(TeamType.STUDY);

        assertThat(result).isSameAs(sections);
        then(teamGetService).should().getTeams(TeamType.STUDY);
    }
}
