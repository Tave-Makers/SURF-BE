package com.tavemakers.surf.domain.reservation.service;

import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationCreateService {

    private final ReservationRepository reservationRepository;

    /** 예약 정보 저장 */
    public Reservation save(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

}
