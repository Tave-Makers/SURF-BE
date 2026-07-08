package com.tavemakers.surf.domain.schedule.presentation.controller;

import static com.tavemakers.surf.domain.schedule.presentation.controller.ResponseMessage.SCHEDULE_UPDATED;

import com.tavemakers.surf.domain.schedule.presentation.dto.request.ScheduleUpdateReqDTO;
import com.tavemakers.surf.domain.schedule.application.usecase.ScheduleUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "일정", description = "일정 관련 API")
public class SchedulePatchController {
    private final ScheduleUsecase scheduleUseCase;

    @Operation(summary = "개별 일정 수정", description = "일정을 수정합니다.")
    @PatchMapping("/v1/admin/schedules/{scheduleId}")
    public ApiResponse<Void> updateSchedule(@PathVariable("scheduleId") Long scheduleId, @RequestBody @Valid
    ScheduleUpdateReqDTO reqDTO) {
        scheduleUseCase.updateSchedule(reqDTO, scheduleId);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_UPDATED.getMessage(), null);
    }
}
