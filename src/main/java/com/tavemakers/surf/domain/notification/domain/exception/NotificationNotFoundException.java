package com.tavemakers.surf.domain.notification.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.notification.domain.exception.ErrorMessage.NOTIFICATION_NOT_FOUND;

public class NotificationNotFoundException extends BaseException {
    public NotificationNotFoundException() {
        super(NOTIFICATION_NOT_FOUND.getStatus(), NOTIFICATION_NOT_FOUND.getMessage());
    }
}
