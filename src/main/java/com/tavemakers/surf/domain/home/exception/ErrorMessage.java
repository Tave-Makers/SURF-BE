package com.tavemakers.surf.domain.home.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    HOME_BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [홈 배너]입니다."),
    ALL_HOME_BANNERS_REQUIRED(HttpStatus.BAD_REQUEST, "모든 [홈 배너]를 입력해야 합니다."),
    EMPTY_HOME_BANNERS(HttpStatus.BAD_REQUEST, "[홈 배너]가 비어있습니다."),
    INVALID_HOME_BANNER_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 [홈 배너] 요청입니다.");

    private final HttpStatus status;
    private final String message;

}