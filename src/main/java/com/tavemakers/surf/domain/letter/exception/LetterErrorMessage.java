package com.tavemakers.surf.domain.letter.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LetterErrorMessage {

    LETTER_MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
