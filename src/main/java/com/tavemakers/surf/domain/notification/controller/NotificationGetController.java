package com.tavemakers.surf.domain.notification.controller;

import com.tavemakers.surf.domain.notification.dto.response.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.service.NotificationService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.tavemakers.surf.domain.notification.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "알람")
public class NotificationGetController {

    private final NotificationService notificationService;
    private final LogEventEmitter logEventEmitter;

    @Operation(summary = "알람 조회", description = "category 파라미터로 카테고리별 필터링 조회합니다. null일 경우 전체 알람 조회")
    @GetMapping("/v1/user/notifications")
    public ApiResponse<List<NotificationResDTO>> getNotifications(
            @RequestParam(required = false) NotificationCategory category
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        logEventEmitter.emit("notification.list_view", Map.of(
                "member_id", memberId,
                "category", category != null ? category.name().toLowerCase() : "all"
        ));
        List<NotificationResDTO> response = notificationService.getNotifications(memberId, category);
        return ApiResponse.response(HttpStatus.OK, NOTIFICATION_READ.getMessage(), response);
    }
}
