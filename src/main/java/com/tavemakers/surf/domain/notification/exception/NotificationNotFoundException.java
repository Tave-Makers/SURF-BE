package com.tavemakers.surf.domain.notification.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.notification.exception.ErrorMessage.NOTIFICATION_NOT_FOUND;

public class NotificationNotFoundException extends BaseException {
    public NotificationNotFoundException() {
        super(NOTIFICATION_NOT_FOUND.getStatus(), NOTIFICATION_NOT_FOUND.getMessage());
    }
}
