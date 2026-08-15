package com.tavemakers.surf.domain.letter.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

/** 차단 관계로 인해 쪽지를 보낼 수 없을 때 발생하는 예외 */
public class LetterBlockedException extends BaseException {

    public LetterBlockedException() {
        super(
                LetterErrorMessage.LETTER_BLOCKED.getStatus(),
                LetterErrorMessage.LETTER_BLOCKED.getMessage()
        );
    }
}
