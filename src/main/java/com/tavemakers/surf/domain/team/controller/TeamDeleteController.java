package com.tavemakers.surf.domain.team.controller;

import com.tavemakers.surf.domain.team.usecase.TeamUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.team.controller.ResponseMessage.TEAM_DELETED;


@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "팀", description = "팀 관련 CRUD API")
public class TeamDeleteController {

    private final TeamUsecase teamUsecase;

    /** 팀 삭제*/
    @Operation(summary = "팀 삭제", description = "특정 ID의 팀을 삭제합니다.")
    @DeleteMapping("/v1/admin/teams/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId) {
        teamUsecase.deleteTeam(teamId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, TEAM_DELETED.getMessage());
    }
}