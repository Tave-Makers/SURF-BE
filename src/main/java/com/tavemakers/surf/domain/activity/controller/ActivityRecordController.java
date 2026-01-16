package com.tavemakers.surf.domain.activity.controller;

import com.tavemakers.surf.domain.activity.dto.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.dto.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.facade.ActivityRecordFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기록")
public class ActivityRecordController {

    private final ActivityRecordFacade activityRecordFacade;

    @Operation(summary = "활동 점수(기록) 부여")
    @PostMapping("/v1/admin/activity-records")
    public ApiResponse<Void> createActivityRecord(@RequestBody @Valid ActivityRecordReqDTO dto) {
        activityRecordFacade.createActivityRecordList(dto);
        return ApiResponse.response(HttpStatus.CREATED, ACTIVITY_RECORD_CREATED.getMessage(), null);
    }

    @Operation(summary = "활동 기록 조회(무한스크롤)")
    @GetMapping("/v1/user/members/activity-records")
    public ApiResponse<ActivityRecordSliceResDTO> getActivityRecord(
            @RequestParam ScoreType scoreType,
            @RequestParam int pageSize,
            @RequestParam int pageNum
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ActivityRecordSliceResDTO response =
                activityRecordFacade.getActivityRecordList(memberId, scoreType, pageSize, pageNum);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_READ.getMessage(), response);
    }

}
