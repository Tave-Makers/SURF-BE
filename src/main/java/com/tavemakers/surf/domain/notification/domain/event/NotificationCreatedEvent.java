package com.tavemakers.surf.domain.notification.domain.event;

public record NotificationCreatedEvent(
        Long notificationId,
        Long receiverId
) {}