package com.tavemakers.surf.domain.letter.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

public class LetterMailSendFailException extends BaseException {

    public LetterMailSendFailException() {
        super(
                LetterErrorMessage.LETTER_MAIL_SEND_FAIL.getStatus(),
                LetterErrorMessage.LETTER_MAIL_SEND_FAIL.getMessage()
        );
    }
}
