package com.tavemakers.surf.domain.notification.controller;

import com.tavemakers.surf.domain.notification.dto.res.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.facade.NotificationFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.notification.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "알람")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    @Operation(summary = "알람 조회", description = "category 파라미터로 카테고리별 필터링 조회합니다. null일 경우 전체 알람 조회")
    @GetMapping("/v1/user/notifications")
    public ApiResponse<List<NotificationResDTO>> getNotifications(
            @RequestParam(required = false) NotificationCategory category
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        List<NotificationResDTO> response = notificationFacade.getNotifications(memberId, category);
        return ApiResponse.response(HttpStatus.OK, NOTIFICATION_READ.getMessage(), response);
    }

    @Operation(summary = "알람 읽음 처리", description = "특정 알람을 읽음 처리합니다.")
    @PatchMapping("/v1/user/notifications/{notificationId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long notificationId
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        notificationFacade.markAsRead(notificationId, memberId);
        return ApiResponse.response(HttpStatus.OK, NOTIFICATION_READ_MARK.getMessage());
    }
}
