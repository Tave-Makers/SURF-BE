package com.tavemakers.surf.domain.activity.controller;

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
public class ActivityRecordDeleteController {

    private final ActivityRecordUsecase activityRecordUsecase;

    @Operation(summary = "활동기록 삭제 (관리자)")
    @DeleteMapping("/v1/admin/activity-records/{activityRecordId}")
    public ApiResponse<Void> deleteActivityRecord(
            @PathVariable Long activityRecordId
    ) {
        activityRecordUsecase.deleteActivityRecord(activityRecordId);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_DELETED.getMessage());
    }

}
