package com.tavemakers.surf.domain.team.controller;

import com.tavemakers.surf.domain.team.dto.response.TeamDetailResDTO;
import com.tavemakers.surf.domain.team.dto.response.TeamGenerationSectionResDTO;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.usecase.TeamUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.team.controller.ResponseMessage.TEAM_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "팀", description = "팀 관련 CRUD API")
public class TeamGetController {

    private final TeamUsecase teamUsecase;

    /** 팀 목록 조회 (파라미터 미입력 시 전체 조회, STUDY, PROJECT로 구분)*/
    @Operation(summary = "팀 목록 조회", description = "팀 목록을 기수별로 구분하여 조회합니다. type 파라미터로 스터디/프로젝트 분리 가능 (STUDY, PROJECT)")
    @GetMapping("/v1/admin/teams")
    public ApiResponse<List<TeamGenerationSectionResDTO>> getTeams(
            @RequestParam(required = false) TeamType type
    ) {
        List<TeamGenerationSectionResDTO> response = teamUsecase.getTeams(type);
        return ApiResponse.response(HttpStatus.OK, TEAM_READ.getMessage(), response);
    }

    /** 팀 상세 조회*/
    @Operation(summary = "팀 상세 조회", description = "팀의 상세 정보를 조회합니다.")
    @GetMapping("/v1/admin/teams/{teamId}")
    public ApiResponse<TeamDetailResDTO> getTeamDetail(@PathVariable Long teamId) {
        TeamDetailResDTO response = teamUsecase.getTeamDetail(teamId);
        return ApiResponse.response(HttpStatus.OK, TEAM_READ.getMessage(), response);
    }
}
