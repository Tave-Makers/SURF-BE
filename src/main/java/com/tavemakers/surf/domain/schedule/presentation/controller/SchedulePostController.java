package com.tavemakers.surf.domain.schedule.presentation.controller;

import static com.tavemakers.surf.domain.schedule.presentation.controller.ResponseMessage.SCHEDULE_CREATED;

import com.tavemakers.surf.domain.schedule.presentation.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.domain.schedule.application.usecase.ScheduleUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "일정", description = "일정 관련 API")
public class SchedulePostController {
    private final ScheduleUsecase scheduleUseCase;

    @Operation(summary = "게시글 작성 시 일정 생성", description = "공지사항 게시글 작성시 일정을 생성합니다.")
    @PostMapping("/v1/admin/posts/{postId}/schedules")
    public ApiResponse<Void> createScheduleAtPost(
            @PathVariable Long postId, @RequestBody @Valid ScheduleCreateReqDTO dto) {
        scheduleUseCase.createScheduleAtPost(dto, postId);
        return ApiResponse.response(HttpStatus.CREATED, SCHEDULE_CREATED.getMessage(),null);
    }

    @Operation(summary = "개별 일정 생성", description = "캘린더에서 일정을 생성합니다.")
    @PostMapping("/v1/admin/calendar/schedules")
    public ApiResponse<Void> createScheduleAtCalendar(
         @RequestBody @Valid ScheduleCreateReqDTO dto) {
        scheduleUseCase.createScheduleSingle(dto);
        return ApiResponse.response(HttpStatus.CREATED, SCHEDULE_CREATED.getMessage(),null);
    }
}
