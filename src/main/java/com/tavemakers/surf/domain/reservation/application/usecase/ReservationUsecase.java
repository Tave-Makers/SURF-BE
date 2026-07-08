package com.tavemakers.surf.domain.reservation.application.usecase;

import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.domain.exception.ReservationAlreadyPublishedException;
import com.tavemakers.surf.domain.reservation.application.query.ReservationGetService;
import com.tavemakers.surf.domain.reservation.domain.service.ReservationCreateService;
import com.tavemakers.surf.domain.reservation.infrastructure.ReservationScheduleService;
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
    private final PostGetService postGetService;

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
        // 게시글 행 락으로 postId 단위 직렬화 — 동시 예약 변경이 RESERVED 를 중복 생성하는 것을 방지
        Post post = postGetService.getPostForUpdate(postId);
        if (!post.isReserved()) {
            throw new ReservationAlreadyPublishedException(); // 발행 완료 글 재예약 → 재발행 방지
        }

        Reservation existed = reservationGetService.findByPostIdAndStatusForUpdate(postId);
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
