package com.tavemakers.surf.domain.reservation.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.reservation.domain.exception.ErrorMessage.RESERVATION_NOT_FOUND;

public class ReservationNotFoundException extends BaseException {
    public ReservationNotFoundException() {
        super(RESERVATION_NOT_FOUND.getStatus(), RESERVATION_NOT_FOUND.getMessage());
    }
}
