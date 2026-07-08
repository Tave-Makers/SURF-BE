package com.tavemakers.surf.domain.post.application.usecase;
import com.tavemakers.surf.domain.post.domain.service.post.PostCreateService;

import com.tavemakers.surf.domain.post.presentation.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.domain.post.presentation.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.reservation.application.usecase.ReservationUsecase;
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

        logEventEmitter.emit("post.create", Map.of(
                "post_id", result.postId(),
                "board_id", req.boardId(),
                "title_length", req.title().length(),
                "has_image", req.hasImage()
        ));

        return result;
    }
}
