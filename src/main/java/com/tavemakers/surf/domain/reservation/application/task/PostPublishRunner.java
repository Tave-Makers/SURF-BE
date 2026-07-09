package com.tavemakers.surf.domain.reservation.application.task;

import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.post.domain.service.post.PostPublishService;
import com.tavemakers.surf.domain.post.domain.event.PostPublishedEvent;
import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.application.query.ReservationGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPublishRunner {

    private final ReservationGetService reservationGetService;
    private final PostGetService postGetService;
    private final PostPublishService postPublishService;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public void publishPost(Long reservationId) {
        Reservation reservation = reservationGetService.getReservationById(reservationId);
        Long postId = reservation.getPostId();

        // 게시글 행 락 — 예약 변경(updateReservationPost)과 같은 순서(post → reservation)로 잠가 데드락을 방지
        if (postGetService.findPostForUpdate(postId).isEmpty()) {
            reservation.cancel();
            log.info("게시글이 삭제되어 예약을 취소합니다. reservationId={}", reservationId);
            return;
        }

        // 락 획득 후 예약 최신 상태 재검증(잠금 읽기) — 락 대기 중 취소·재예약된 예약이면 발행하지 않는다
        Reservation current = reservationGetService.findByPostIdAndStatusForUpdate(postId);
        if (current == null || !current.getId().equals(reservationId)) {
            log.info("예약이 취소 또는 변경되어 발행을 건너뜁니다. reservationId={}", reservationId);
            return;
        }

        // 발행 — 이미 발행된 게시글이면 멱등 no-op (중복 발행·중복 알림 방지)
        if (!postPublishService.publishReservedPost(postId)) {
            return;
        }
        current.publish();

        eventPublisher.publishEvent(
                new PostPublishedEvent(postId)
        );

        log.info("예약 번호 {}번 예약 작업 수행", reservationId);
    }

}
