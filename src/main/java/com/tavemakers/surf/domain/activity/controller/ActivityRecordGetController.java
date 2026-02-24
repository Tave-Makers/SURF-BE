package com.tavemakers.surf.domain.activity.controller;

import com.tavemakers.surf.domain.activity.dto.response.AdminActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.usecase.ActivityRecordUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기록 관리")
public class ActivityRecordGetController {

    private final ActivityRecordUsecase activityRecordUsecase;

    /** 특정 회원의 활동기록 조회 (관리자) */
    @Operation(summary = "특정 회원의 활동기록 조회 (관리자)")
    @GetMapping("/v1/admin/scores/members/{memberId}/activity-records")
    public ApiResponse<AdminActivityRecordSliceResDTO> getAdminActivityRecords(
            @PathVariable Long memberId,
            @RequestParam int pageNum,
            @RequestParam int pageSize
    ) {
        AdminActivityRecordSliceResDTO response = activityRecordUsecase.getAdminActivityRecordList(memberId, pageNum, pageSize);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_READ.getMessage(), response);
    }

    /** 특정 팀의 활동기록 조회 (관리자) */
    @Operation(summary = "특정 팀의 활동기록 조회 (관리자)")
    @GetMapping("/v1/admin/scores/teams/{teamId}/activity-records")
    public ApiResponse<AdminActivityRecordSliceResDTO> getAdminTeamActivityRecords(
            @PathVariable Long teamId,
            @RequestParam int pageNum,
            @RequestParam int pageSize
    ) {
        AdminActivityRecordSliceResDTO response = activityRecordUsecase.getAdminTeamActivityRecordList(teamId, pageNum, pageSize);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_READ.getMessage(), response);
    }

}
