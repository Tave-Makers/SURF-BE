package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.MIS_MATCH_PASSWORD;

public class MisMatchPasswordException extends BaseException {
    public MisMatchPasswordException() {
        super(MIS_MATCH_PASSWORD.getStatus(), MIS_MATCH_PASSWORD.getMessage());
    }
}
