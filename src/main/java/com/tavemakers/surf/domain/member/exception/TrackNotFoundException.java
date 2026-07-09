package com.tavemakers.surf.domain.member.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.member.exception.ErrorMessage.TRACK_NOT_FOUND;

public class TrackNotFoundException extends BaseException {

    public TrackNotFoundException(String s) {
        super(TRACK_NOT_FOUND.getStatus(), TRACK_NOT_FOUND.getMessage());
    }

    public TrackNotFoundException() {
        super(TRACK_NOT_FOUND.getStatus(), TRACK_NOT_FOUND.getMessage());
    }

}
