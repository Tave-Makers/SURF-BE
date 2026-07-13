package com.tavemakers.surf.domain.schedule.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    SCHEDULE_TIME_ERROR(HttpStatus.BAD_REQUEST,"일정 시작 시간은 종료 시간보다 이전이어야 합니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [일정]입니다.")
    ;

    private final HttpStatus status;
    private final String message;

}
