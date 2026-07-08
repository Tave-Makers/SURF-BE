package com.tavemakers.surf.domain.reservation.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.reservation.domain.exception.ErrorMessage.RESERVATION_ALREADY_PUBLISHED;

public class ReservationAlreadyPublishedException extends BaseException {
    public ReservationAlreadyPublishedException() {
        super(RESERVATION_ALREADY_PUBLISHED.getStatus(), RESERVATION_ALREADY_PUBLISHED.getMessage());
    }
}
