package com.tavemakers.surf.application.post.usecase;
import com.tavemakers.surf.domain.post.service.post.PostPatchService;

import com.tavemakers.surf.presentation.post.dto.request.PostUpdateReqDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.application.reservation.usecase.ReservationUsecase;
import com.tavemakers.surf.global.common.moderation.MaskingResult;
import com.tavemakers.surf.global.common.moderation.ProfanityMasker;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 게시글 수정 Usecase */
@Service
@RequiredArgsConstructor
public class PostPatchUsecase {

    private final PostPatchService postPatchService;
    private final ReservationUsecase reservationUsecase;
    private final ProfanityMasker profanityMasker;
    private final LogEventEmitter logEventEmitter;

    /** 게시글 수정 (예약 시간 변경 포함) — 제목·본문은 마스킹된 사본으로 전달한다 */
    @Transactional
    public PostDetailResDTO updatePost(Long postId, PostUpdateReqDTO req, Long memberId) {
        if (Boolean.TRUE.equals(req.isReservationChanged())) {
            reservationUsecase.updateReservationPost(postId, req.reservedAt());
        }
        PostUpdateReqDTO masked = req.withMaskedText(
                maskAndLog(req.title(), "post.title"), maskAndLog(req.content(), "post.content"));
        return postPatchService.updatePost(postId, masked, memberId);
    }

    /** 금칙어를 마스킹하고, 실제로 가려진 경우에만 로그를 남긴다 — 원문·본문은 로그에 남기지 않는다 */
    private String maskAndLog(String text, String target) {
        MaskingResult result = profanityMasker.maskWithResult(text);
        if (result.matchCount() > 0) {
            logEventEmitter.emit("moderation.masked", Map.of(
                    "target", target,
                    "match_count", result.matchCount(),
                    "matched", result.matched()));
        }
        return result.masked();
    }
}
