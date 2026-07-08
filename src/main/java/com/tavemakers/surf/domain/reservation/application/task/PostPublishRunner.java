package com.tavemakers.surf.domain.reservation.application.task;

import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.repository.PostRepository;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import com.tavemakers.surf.domain.post.domain.service.support.PostPublishedEvent;
import com.tavemakers.surf.domain.reservation.domain.entity.Reservation;
import com.tavemakers.surf.domain.reservation.application.query.ReservationGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPublishRunner {

    private final ReservationGetService reservationGetService;
    private final PostGetService postGetService;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public void publishPost(Long reservationId) {
        Reservation reservation = reservationGetService.getReservationById(reservationId);
        Post post = getPost(reservation);
        if (post == null) {
            reservation.cancel();
            log.info("게시글이 삭제되어 예약을 취소합니다. reservationId={}", reservationId);
            return;
        }

        post.publish();
        reservation.publish();

        eventPublisher.publishEvent(
                new PostPublishedEvent(post.getId())
        );

        log.info("예약 번호 {}번 예약 작업 수행", reservationId);
    }

    private @Nullable Post getPost(Reservation reservation) {
        return postGetService.findPost(reservation.getPostId())
                .orElse(null);
    }

}
