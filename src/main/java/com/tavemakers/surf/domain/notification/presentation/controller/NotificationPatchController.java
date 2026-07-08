package com.tavemakers.surf.domain.notification.presentation.controller;

import com.tavemakers.surf.domain.notification.domain.service.NotificationService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.notification.presentation.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "알람")
public class NotificationPatchController {

    private final NotificationService notificationService;

    @Operation(summary = "알람 읽음 처리", description = "특정 알람을 읽음 처리합니다.")
    @PatchMapping("/v1/user/notifications/{notificationId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long notificationId
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        notificationService.markAsRead(notificationId, memberId);
        return ApiResponse.response(HttpStatus.OK, NOTIFICATION_READ_MARK.getMessage());
    }
}
