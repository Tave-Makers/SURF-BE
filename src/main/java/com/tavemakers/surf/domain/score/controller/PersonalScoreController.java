package com.tavemakers.surf.domain.score.controller;

import com.tavemakers.surf.domain.score.dto.response.PersonalScoreWithPinnedResDto;
import com.tavemakers.surf.domain.score.dto.response.ScoreSliceResDTO;
import com.tavemakers.surf.domain.score.usecase.PersonalScoreUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.score.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동점수")
public class PersonalScoreController {

    private final PersonalScoreUsecase personalScoreUsecase;

    @Operation(
            summary = "[활동점수] + 고정 5개[활동기록] 조회)",
            description = "[활동점수] + 고정 5개[활동기록] 조회"
    )
    @GetMapping("/v1/user/members/personal-score/pinned5")
    public ApiResponse<PersonalScoreWithPinnedResDto> getScoreAndPinned5(
    ) {
        PersonalScoreWithPinnedResDto response =
                personalScoreUsecase.findPersonalScoreAndPinned(SecurityUtils.getCurrentMemberId());
        return ApiResponse.response(HttpStatus.OK, SCORE_AND_PINNED_READ.getMessage(), response);
    }

    @GetMapping("/v1/manager/personal-score")
    public ApiResponse<ScoreSliceResDTO> getPersonalActivityScoreList(
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        ScoreSliceResDTO data = personalScoreUsecase.readPersonalScore(pageNumber, pageSize);
        return ApiResponse.response(HttpStatus.OK, PERSONAL_ACTIVITY_SCORE_LIST_READ.getMessage(), data);
    }

    @GetMapping("/v1/manager/team-score")
    public ApiResponse<ScoreSliceResDTO> getTeamActivityScoreList(
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        ScoreSliceResDTO data = personalScoreUsecase.readTeamScore(pageNumber, pageSize);
        return ApiResponse.response(HttpStatus.OK, TEAM_ACTIVITY_SCORE_LIST_READ.getMessage(), data);
    }

}
