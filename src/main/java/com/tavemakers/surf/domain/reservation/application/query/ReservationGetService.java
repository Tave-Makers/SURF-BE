package com.tavemakers.surf.domain.reservation.application.query;

import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.domain.entity.ReservationStatus;
import com.tavemakers.surf.domain.reservation.domain.exception.ReservationNotFoundException;
import com.tavemakers.surf.domain.reservation.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationGetService {

    private final ReservationRepository reservationRepository;

    /** ID로 예약 정보 조회 */
    public Reservation getReservationById(Long id) {
        return reservationRepository.findByIdAndStatus(id, ReservationStatus.RESERVED)
                .orElseThrow(ReservationNotFoundException::new);
    }

    /** 전체 예약 목록 조회 */
    public List<Reservation> getAllReservation() {
        return reservationRepository.findByStatus(ReservationStatus.RESERVED);
    }

    /** 게시글 ID로 예약 정보 조회 */
    public Reservation findByPostIdAndStatus(Long postId) {
        return reservationRepository.findByPostIdAndStatus(postId, ReservationStatus.RESERVED)
                .orElse(null);
    }

    /** 게시글 ID로 예약 정보 잠금 조회 (예약 변경 직렬화용) */
    public Reservation findByPostIdAndStatusForUpdate(Long postId) {
        return reservationRepository.findByPostIdAndStatusForUpdate(postId, ReservationStatus.RESERVED)
                .orElse(null);
    }

}
