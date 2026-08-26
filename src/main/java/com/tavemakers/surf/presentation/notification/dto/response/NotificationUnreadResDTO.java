package com.tavemakers.surf.presentation.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "안 읽은 알림 존재 여부 응답 DTO")
public record NotificationUnreadResDTO(
        @Schema(description = "안 읽은 알림 존재 여부", example = "true")
        boolean hasUnread
) {
    public static NotificationUnreadResDTO from(boolean hasUnread) {
        return new NotificationUnreadResDTO(hasUnread);
    }
}
