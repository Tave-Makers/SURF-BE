package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.TRACK_ALREADY_EXISTS;

public class TrackAlreadyExistsException extends BaseException {

    public TrackAlreadyExistsException() {
        super(TRACK_ALREADY_EXISTS.getStatus(), TRACK_ALREADY_EXISTS.getMessage());
    }
}
