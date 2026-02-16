package com.tavemakers.surf.domain.team.controller;

import com.tavemakers.surf.domain.team.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.domain.team.dto.response.TeamResDTO;
import com.tavemakers.surf.domain.team.service.TeamService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.team.controller.ResponseMessage.TEAM_UPDATED;


@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "팀", description = "팀 관련 CRUD API")
public class TeamPutController {

    private final TeamService teamService;

    /** 팀 정보 수정 (팀장 memberId(leaderMemberId)는 팀원 memberId 리스트(memberIds) 내에 포함되어야 합니다.)*/
    @Operation(summary = "팀 수정", description = "기존 팀 정보를 새로운 정보로 완전히 대체합니다.")
    @PutMapping("/v1/admin/teams/{teamId}")
    public ApiResponse<TeamResDTO> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpsertReqDTO req
    ) {
        TeamResDTO response = teamService.updateTeam(teamId, req);
        return ApiResponse.response(HttpStatus.OK, TEAM_UPDATED.getMessage(), response);
    }
}
