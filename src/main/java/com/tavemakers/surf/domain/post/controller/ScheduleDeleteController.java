package com.tavemakers.surf.domain.post.controller;

import static com.tavemakers.surf.domain.post.controller.ResponseMessage.SCHEDULE_DELETED;

import com.tavemakers.surf.domain.post.facade.ScheduleFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "일정", description = "일정 관련 API")
public class ScheduleDeleteController {
    private final ScheduleFacade scheduleFacade;

    @Operation(summary = "개별 일정 삭제", description = "일정을 삭제합니다.")
    @DeleteMapping("/v1/admin/schedules/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(@PathVariable("scheduleId") Long scheduleId) {
        scheduleFacade.deleteSchedule(scheduleId);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_DELETED.getMessage(), null);
    }

    @Operation(summary = "게시글과 매핑된 일정 삭제", description = "일정을 삭제합니다.")
    @DeleteMapping("/v1/admin/posts/{postId}/schedules/{scheduleId}")
    public ApiResponse<Void> deleteScheduleAtPost(
            @PathVariable("postId") Long postId,
            @PathVariable("scheduleId") Long scheduleId) {
        scheduleFacade.deleteScheduleAtPost(postId, scheduleId);
        return ApiResponse.response(HttpStatus.OK, SCHEDULE_DELETED.getMessage(), null);
    }
}
