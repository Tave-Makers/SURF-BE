package com.tavemakers.surf.presentation.notification.controller;

import com.tavemakers.surf.presentation.notification.dto.response.NotificationSliceResDTO;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.application.notification.usecase.NotificationUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tavemakers.surf.presentation.notification.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "알람")
public class NotificationGetController {

    private final NotificationUsecase notificationUsecase;
    private final LogEventEmitter logEventEmitter;

    @Operation(summary = "알람 조회", description = "category 파라미터로 카테고리별 필터링 조회합니다. null일 경우 전체 알람 조회. page/size로 무한스크롤 조회합니다.")
    @GetMapping("/v1/user/notifications")
    public ApiResponse<NotificationSliceResDTO> getNotifications(
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        logEventEmitter.emit("notification.list_view", Map.of(
                "member_id", memberId,
                "category", category != null ? category.name().toLowerCase() : "all"
        ));
        NotificationSliceResDTO response =
                notificationUsecase.getNotifications(memberId, category, PageRequest.of(page, size));
        return ApiResponse.response(HttpStatus.OK, NOTIFICATION_READ.getMessage(), response);
    }
}
