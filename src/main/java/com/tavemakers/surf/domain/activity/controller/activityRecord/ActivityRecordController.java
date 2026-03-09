package com.tavemakers.surf.domain.activity.controller.activityRecord;

import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordReqDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.request.ActivityRecordReqDTOV2;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityCategoryDetailResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityCategoryResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityRecordSliceResDTO;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.usecase.ActivityRecordUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기록")
public class ActivityRecordController {

    private final ActivityRecordUsecase activityRecordUsecase;

    @Operation(summary = "활동 점수(기록) 부여")
    @PostMapping("/v1/admin/activity-records")
    public ApiResponse<Void> createActivityRecord(@RequestBody @Valid ActivityRecordReqDTO dto) {
        activityRecordUsecase.createActivityRecordList(dto);
        return ApiResponse.response(HttpStatus.CREATED, ACTIVITY_RECORD_CREATED.getMessage(), null);
    }

    @Operation(summary = "활동 점수(기록) 부여 Version 2")
    @PostMapping("/v1/manager/activity-records")
    public ApiResponse<Void> applyActivityRecord(
            @RequestBody @Valid ActivityRecordReqDTOV2 dto
    ) {
        activityRecordUsecase.applyActivityRecord(dto);
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
                activityRecordUsecase.getActivityRecordList(memberId, scoreType, pageSize, pageNum);
        return ApiResponse.response(HttpStatus.OK, ACTIVITY_RECORD_READ.getMessage(), response);
    }

    @Operation(summary = "활동 종류 조회")
    @GetMapping("/v1/manager/activity-types")
    public ApiResponse<List<ActivityCategoryDetailResDTO>> getAllActivityTypeInformation() {
        List<ActivityCategoryDetailResDTO> data = activityRecordUsecase.getAllActivityTypeInformation();
        return ApiResponse.response(HttpStatus.OK, ALL_ACTIVITY_TYPE_READ.getMessage(), data);
    }

    @Operation(summary = "활동 종류의 카테고리들만 조회 (카테고리에 포함된 활동 종류는 포함 X)")
    @GetMapping("/v1/manager/activity-categories")
    public ApiResponse<List<ActivityCategoryResDTO>> getAllActivityCategoriesInformation() {
        List<ActivityCategoryResDTO> data = activityRecordUsecase.getAllActivityCategoriesInformation();
        return ApiResponse.response(HttpStatus.OK, ALL_ACTIVITY_CATEGORY_READ.getMessage(), data);
    }

    @Operation(summary = "특정 카테고리의 활동 종류 조회")
    @GetMapping("/v1/manager/activity-type")
    public ApiResponse<ActivityCategoryDetailResDTO> getActivityTypeInformationByCategory(
            @RequestParam String category
    ) {
        ActivityCategoryDetailResDTO data = activityRecordUsecase.getActivityTypeInformationByCategory(category);
        return ApiResponse.response(HttpStatus.OK, SPECIFIC_ACTIVITY_CATEGORY_READ.getMessage(), data);
    }

}
