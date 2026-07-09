package com.tavemakers.surf.domain.score.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.score.exception.ErrorMessage.PERSONAL_SCORE_NOT_FOUND;

public class PersonalScoreNotFoundException extends BaseException {
    public PersonalScoreNotFoundException() {
        super(PERSONAL_SCORE_NOT_FOUND.getStatus(), PERSONAL_SCORE_NOT_FOUND.getMessage());
    }
}
