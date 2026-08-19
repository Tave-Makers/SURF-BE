package com.tavemakers.surf.domain.activity.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    ACTIVITY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [활동기록]입니다."),
    ACTIVITY_RECORD_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 [활동기록]입니다."),
    INACTIVE_MEMBER_INCLUDED(HttpStatus.BAD_REQUEST, "비활동 [멤버]가 포함되어 [활동점수]를 부여할 수 없습니다."),

    ACTIVE_GENERATION_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "[활동 기수]가 초기화되지 않았습니다.");

    private final HttpStatus status;
    private final String message;

}
