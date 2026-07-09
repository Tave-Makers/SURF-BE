package com.tavemakers.surf.domain.score.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    PERSONAL_SCORE_NOT_FOUND(HttpStatus.BAD_REQUEST, "[개인활동점수]를 조회할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

}
