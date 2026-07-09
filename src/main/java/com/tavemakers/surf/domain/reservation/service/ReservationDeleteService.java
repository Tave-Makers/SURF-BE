package com.tavemakers.surf.domain.reservation.service;

import com.tavemakers.surf.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationDeleteService {

    private final ReservationRepository reservationRepository;

    /** 게시글의 예약 정보 삭제 — 게시글 삭제와 같은 트랜잭션에서 정합을 유지한다 (호출자가 트랜잭션을 연다) */
    public void deleteByPostId(Long postId) {
        reservationRepository.deleteByPostId(postId);
    }

}
