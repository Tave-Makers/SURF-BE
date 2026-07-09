package com.tavemakers.surf.domain.notification.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 [알람]을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

}