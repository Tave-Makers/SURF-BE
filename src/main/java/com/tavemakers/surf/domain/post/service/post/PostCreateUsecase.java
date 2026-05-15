package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.domain.post.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.reservation.usecase.ReservationUsecase;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 게시글 생성 Usecase */
@Service
@RequiredArgsConstructor
public class PostCreateUsecase {

    private final PostCreateService postCreateService;
    private final ReservationUsecase reservationUsecase;
    private final LogEventEmitter logEventEmitter;

    /** 게시글 생성 (예약 포함) */
    @Transactional
    public PostDetailResDTO createPost(PostCreateReqDTO req, Long memberId) {
        PostDetailResDTO result = postCreateService.createPost(req, memberId);
        if (req.isReserved()) {
            reservationUsecase.reservePost(result.postId(), req.reservedAt());
        }

        logEventEmitter.emit("post_create", Map.of(
                "post_id", result.postId(),
                "board_id", req.boardId(),
                "title_length", req.title().length(),
                "has_Fimage", req.hasImage()
        ));

        return result;
    }
}
