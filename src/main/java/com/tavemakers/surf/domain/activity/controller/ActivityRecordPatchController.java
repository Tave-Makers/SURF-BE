package com.tavemakers.surf.domain.activity.controller;

import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordPatchReqDTO;
import com.tavemakers.surf.domain.activity.usecase.ActivityRecordUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기록 관리")
public class ActivityRecordPatchController {

    private final ActivityRecordUsecase activityRecordUsecase;

    /** 활동기록 수정 (관리자) */
    @Operation(summary = "활동기록 수정 (관리자)")
    @PatchMapping("/v1/admin/activity-records/{activityRecordId}")
    public ApiResponse<Void> patchActivityRecord(
            @PathVariable Long activityRecordId,
            @Valid @RequestBody ActivityRecordPatchReqDTO dto
    ) {
        activityRecordUsecase.patchActivityRecord(activityRecordId, dto);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_UPDATED.getMessage());
    }

}
