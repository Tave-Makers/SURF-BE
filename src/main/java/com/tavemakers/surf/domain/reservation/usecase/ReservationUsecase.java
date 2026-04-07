package com.tavemakers.surf.domain.reservation.usecase;

import com.tavemakers.surf.domain.reservation.entity.Reservation;
import com.tavemakers.surf.domain.reservation.service.ReservationGetService;
import com.tavemakers.surf.domain.reservation.service.ReservationCreateService;
import com.tavemakers.surf.domain.reservation.service.ReservationScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationUsecase {

    private final ReservationCreateService reservationCreateService;
    private final ReservationGetService reservationGetService;
    private final ReservationScheduleService scheduleService;

    /** 게시글 예약 발행 등록 */
    @Transactional
    public void reservePost(Long postId, LocalDateTime reservedAt) {
        Instant publishAt = toInstant(reservedAt);
        Reservation reservation = Reservation.of(postId, publishAt);
        Reservation savedReservation = reservationCreateService.save(reservation);
        scheduleService.schedule(savedReservation.getId(), publishAt);
    }

    /** 게시글의 예약 시간 조회 */
    @Transactional(readOnly = true)
    public LocalDateTime getReservedAt(Long postId) {
        Reservation existed = reservationGetService.findByPostIdAndStatus(postId);
        if (existed == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
                existed.getReservedAt(),
                ZoneId.of("Asia/Seoul")
        );
    }

    /** 예약 발행 시간 변경 */
    @Transactional
    public void updateReservationPost(Long postId, LocalDateTime changedAt) {
        Reservation existed = reservationGetService.findByPostIdAndStatus(postId);
        if(existed != null) {
            existed.cancel();
        }

        Instant publishAt = toInstant(changedAt);
        Reservation reservation = Reservation.of(postId, publishAt);
        Reservation saved = reservationCreateService.save(reservation);
        scheduleService.schedule(saved.getId(), publishAt);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }

}
