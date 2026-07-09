package com.tavemakers.surf.domain.feedback.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    TOO_MANY_FEEDBACK(HttpStatus.TOO_MANY_REQUESTS, "하루에 3개의 [피드백]만 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

}