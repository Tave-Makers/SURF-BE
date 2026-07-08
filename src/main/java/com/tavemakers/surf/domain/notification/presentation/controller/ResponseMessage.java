package com.tavemakers.surf.domain.notification.presentation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    NOTIFICATION_READ("[알람]이 성공적으로 조회되었습니다."),
    NOTIFICATION_READ_MARK("[알람]이 성공적으로 읽음 처리되었습니다."),

    FCM_TEST_SUCCESS("[FCM] 알람 테스트 성공하였습니다."),
    DEVICE_TOKEN_SUCCESS("[디바이스 토큰]이 성공적으로 등록되었습니다.");

    private final String message;
}
