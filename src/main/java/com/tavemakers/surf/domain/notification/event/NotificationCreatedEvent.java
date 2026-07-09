package com.tavemakers.surf.domain.notification.event;

public record NotificationCreatedEvent(
        Long notificationId,
        Long receiverId
) {}