package com.tavemakers.surf.domain.team.application.usecase;

import com.tavemakers.surf.domain.team.presentation.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.domain.team.presentation.dto.response.TeamDetailResDTO;
import com.tavemakers.surf.domain.team.presentation.dto.response.TeamGenerationSectionResDTO;
import com.tavemakers.surf.domain.team.presentation.dto.response.TeamResDTO;
import com.tavemakers.surf.domain.team.domain.entity.TeamType;
import com.tavemakers.surf.domain.team.domain.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 팀 Usecase */
@Service
@RequiredArgsConstructor
public class TeamUsecase {

    private final TeamService teamService;

    /** 팀 목록 조회 */
    @Transactional(readOnly = true)
    public List<TeamGenerationSectionResDTO> getTeams(TeamType type) {
        return teamService.getTeams(type);
    }

    /** 팀 상세 조회 */
    @Transactional(readOnly = true)
    public TeamDetailResDTO getTeamDetail(Long teamId) {
        return teamService.getTeamDetail(teamId);
    }

    /** 팀 생성 */
    @Transactional
    public TeamResDTO createTeam(TeamUpsertReqDTO req) {
        return teamService.createTeam(req);
    }

    /** 팀 수정 */
    @Transactional
    public TeamResDTO updateTeam(Long teamId, TeamUpsertReqDTO req) {
        return teamService.updateTeam(teamId, req);
    }

    /** 팀 삭제 */
    @Transactional
    public void deleteTeam(Long teamId) {
        teamService.deleteTeam(teamId);
    }
}
