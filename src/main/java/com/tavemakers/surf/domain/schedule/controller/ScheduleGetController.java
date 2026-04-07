package com.tavemakers.surf.domain.schedule.controller;

import static com.tavemakers.surf.domain.schedule.controller.ResponseMessage.SCHEDULE_CALENDAR_READ;
import static com.tavemakers.surf.domain.schedule.controller.ResponseMessage.SCHEDULE_POST_READ;
import static com.tavemakers.surf.domain.schedule.controller.ResponseMessage.SCHEDULE_READ;

import com.tavemakers.surf.domain.schedule.dto.response.ScheduleMonthlyResDTO;
import com.tavemakers.surf.domain.schedule.dto.response.ScheduleResDTO;
import com.tavemakers.surf.domain.schedule.service.ScheduleUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "일정", description = "일정 관련 API")
public class ScheduleGetController {

    private final ScheduleUsecase scheduleUseCase;

    /** 월별 일정 목록 조회 */
    @Operation(summary = "캘린더에 월별 일정 목록 조회", description = "캘린더 페이지에서 월별 일정을 조회합니다.")
    @GetMapping("/v1/user/calendar/schedules")
    public ApiResponse<ScheduleMonthlyResDTO> getMonthlySchedules(
            @RequestParam @Parameter int year, @RequestParam @Parameter int month) {
        String memberRole = SecurityUtils.getCurrentMemberRole();
        ScheduleMonthlyResDTO dto = scheduleUseCase.getScheduleMonthly(memberRole, year, month);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_CALENDAR_READ.getMessage(),dto);
    }

    @Operation(summary = "특정 게시글 일정 조회", description = "특정 게시글에 매핑된 일정이 있을 경우 반환")
    @GetMapping("/v1/user/post/{postId}/schedule")
    public ApiResponse<ScheduleResDTO> getScheduleByPost(@PathVariable Long postId) {
        ScheduleResDTO dto = scheduleUseCase.getScheduleByPost(postId);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_POST_READ.getMessage(),dto);
    }

    /** 특정 일정 단건 조회 (캘린더 수정/삭제용) */
    @Operation(summary = "특정 일정 조회", description = "특정 일정을 캘린더에서 수정/삭제할 때 조회")
    @GetMapping("/v1/admin/calendar/schedules/{scheduleId}")
    public ApiResponse<ScheduleResDTO> getScheduleByScheduleId(@PathVariable Long scheduleId) {
        ScheduleResDTO dto = scheduleUseCase.getScheduleSingleAtCalendar(scheduleId);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_READ.getMessage(),dto);
    }
}
