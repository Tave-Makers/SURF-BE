package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.post.dto.request.PostUpdateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.reservation.usecase.ReservationUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 수정 Usecase */
@Service
@RequiredArgsConstructor
public class PostPatchUsecase {

    private final PostPatchService postPatchService;
    private final ReservationUsecase reservationUsecase;

    /** 게시글 수정 (예약 시간 변경 포함) */
    @Transactional
    public PostDetailResDTO updatePost(Long postId, PostUpdateReqDTO req, Long memberId) {
        if (Boolean.TRUE.equals(req.isReservationChanged())) {
            reservationUsecase.updateReservationPost(postId, req.reservedAt());
        }
        return postPatchService.updatePost(postId, req, memberId);
    }
}
