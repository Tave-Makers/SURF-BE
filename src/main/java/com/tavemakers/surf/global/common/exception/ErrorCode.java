package com.tavemakers.surf.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 4XX Errors
    METHOD_ARGUMENT_NOT_VALID(HttpStatus.BAD_REQUEST, "잘못된 [인자]입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 [RESOURCE, URL]를 찾을 수 없습니다."),
    PARAMETER_NOT_FOUND(HttpStatus.BAD_REQUEST, "요청에 [Parameter]가 존재하지 않습니다."),
    MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "요청 [본문]을 읽을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "[접근 권한]이 없습니다."),

    // 5XX Errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "[Server] 내부 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

}
