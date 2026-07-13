package com.tavemakers.surf.application.team.usecase;

import com.tavemakers.surf.presentation.team.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamDetailResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamGenerationSectionResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamResDTO;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.application.team.query.TeamGetService;
import com.tavemakers.surf.domain.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 팀 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class TeamUsecase {

    private final TeamService teamService;
    private final TeamGetService teamGetService;

    /** 팀 목록 조회 */
    @Transactional(readOnly = true)
    public List<TeamGenerationSectionResDTO> getTeams(TeamType type) {
        return teamGetService.getTeams(type);
    }

    /** 팀 상세 조회 */
    @Transactional(readOnly = true)
    public TeamDetailResDTO getTeamDetail(Long teamId) {
        return teamGetService.getTeamDetail(teamId);
    }

    /** 팀 생성 */
    @Transactional
    public TeamResDTO createTeam(TeamUpsertReqDTO req) {
        return TeamResDTO.from(teamService.createTeam(
                req.generation(), req.type(), req.name(), req.description(),
                req.leaderMemberId(), req.memberIds()));
    }

    /** 팀 수정 */
    @Transactional
    public TeamResDTO updateTeam(Long teamId, TeamUpsertReqDTO req) {
        return TeamResDTO.from(teamService.updateTeam(
                teamId, req.generation(), req.type(), req.name(), req.description(),
                req.leaderMemberId(), req.memberIds()));
    }

    /** 팀 삭제 */
    @Transactional
    public void deleteTeam(Long teamId) {
        teamService.deleteTeam(teamId);
    }
}
