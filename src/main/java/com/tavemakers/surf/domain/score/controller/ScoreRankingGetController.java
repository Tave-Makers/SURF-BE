package com.tavemakers.surf.domain.score.controller;

import com.tavemakers.surf.domain.activity.dto.response.TeamMemberScoreListResDTO;
import com.tavemakers.surf.domain.score.dto.response.MemberScoreRankingSliceResDTO;
import com.tavemakers.surf.domain.score.dto.response.TeamScoreRankingSliceResDTO;
import com.tavemakers.surf.domain.score.usecase.ScoreRankingUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.score.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동점수 랭킹")
public class ScoreRankingGetController {

    private final ScoreRankingUsecase scoreRankingUsecase;

    /** 개인별 상/벌점 현황 조회 */
    @Operation(
            summary = "개인별 상/벌점 현황 조회",
            description = "활동 멤버들의 개인별 상/벌점 현황을 totalScore 내림차순으로 조회합니다."
    )
    @GetMapping("/v1/admin/scores/members")
    public ApiResponse<MemberScoreRankingSliceResDTO> getMemberScoreRanking(
            @RequestParam int pageNum,
            @RequestParam int pageSize
    ) {
        MemberScoreRankingSliceResDTO response = scoreRankingUsecase.getMemberScoreRanking(pageNum, pageSize);
        return ApiResponse.response(HttpStatus.OK, MEMBER_SCORE_RANKING_READ.getMessage(), response);
    }

    /** 팀별 상/벌점 현황 조회 */
    @Operation(
            summary = "팀별 상/벌점 현황 조회",
            description = "팀별 상/벌점 현황을 totalScore 내림차순으로 조회합니다. generation 파라미터로 기수 필터링이 가능합니다."
    )
    @GetMapping("/v1/admin/scores/teams")
    public ApiResponse<TeamScoreRankingSliceResDTO> getTeamScoreRanking(
            @RequestParam(required = false) Integer generation,
            @RequestParam int pageNum,
            @RequestParam int pageSize
    ) {
        TeamScoreRankingSliceResDTO response = scoreRankingUsecase.getTeamScoreRanking(generation, pageNum, pageSize);
        return ApiResponse.response(HttpStatus.OK, TEAM_SCORE_RANKING_READ.getMessage(), response);
    }

    /** 특정 팀의 멤버별 점수 현황 조회 (관리자) */
    @Operation(summary = "특정 팀의 멤버별 점수 현황 조회 (관리자)")
    @GetMapping("/v1/admin/scores/teams/{teamId}/members")
    public ApiResponse<TeamMemberScoreListResDTO> getTeamMemberScores(
            @PathVariable Long teamId
    ) {
        TeamMemberScoreListResDTO response = scoreRankingUsecase.getTeamMemberScores(teamId);
        return ApiResponse.response(HttpStatus.OK, TEAM_MEMBER_SCORE_READ.getMessage(), response);
    }

}
