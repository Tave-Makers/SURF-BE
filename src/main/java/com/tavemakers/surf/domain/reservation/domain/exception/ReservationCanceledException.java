package com.tavemakers.surf.domain.reservation.domain.exception;

import com.tavemakers.surf.global.common.exception.BaseException;

import static com.tavemakers.surf.domain.reservation.domain.exception.ErrorMessage.RESERVATION_CANCELED_EXCEPTION;

public class ReservationCanceledException extends BaseException {
    public ReservationCanceledException() {
        super(RESERVATION_CANCELED_EXCEPTION.getStatus(), RESERVATION_CANCELED_EXCEPTION.getMessage());
    }
}
