package com.tavemakers.surf.domain.team.controller;

import com.tavemakers.surf.domain.team.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.domain.team.dto.response.TeamResDTO;
import com.tavemakers.surf.domain.team.usecase.TeamUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.team.controller.ResponseMessage.TEAM_CREATED;


@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "팀", description = "팀 관련 CRUD API")
public class TeamCreateController {

    private final TeamUsecase teamUsecase;

    /** 팀 생성 (팀장 memberId(leaderMemberId)는 팀원 memberId 리스트(memberIds) 내에 포함되어야 합니다.)*/
    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다.")
    @PostMapping("/v1/admin/teams")
    public ApiResponse<TeamResDTO> createTeam(
            @Valid @RequestBody TeamUpsertReqDTO req
    ) {

        TeamResDTO response = teamUsecase.createTeam(req);
        return ApiResponse.response(HttpStatus.CREATED, TEAM_CREATED.getMessage(), response);
    }
}
