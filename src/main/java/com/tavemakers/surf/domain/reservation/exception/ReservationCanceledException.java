package com.tavemakers.surf.domain.reservation.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.reservation.exception.ErrorMessage.RESERVATION_CANCELED_EXCEPTION;

public class ReservationCanceledException extends BaseException {
    public ReservationCanceledException() {
        super(RESERVATION_CANCELED_EXCEPTION.getStatus(), RESERVATION_CANCELED_EXCEPTION.getMessage());
    }
}
