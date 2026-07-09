package com.tavemakers.surf.application.feedback.usecase;

import com.tavemakers.surf.presentation.feedback.dto.request.FeedbackCreateReqDTO;
import com.tavemakers.surf.presentation.feedback.dto.response.FeedbackResDTO;
import com.tavemakers.surf.domain.feedback.service.FeedbackService;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드백 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class FeedbackUsecase {

    private final FeedbackService feedbackService;

    /** 피드백 생성 */
    @Transactional
    @LogEvent(value = "feedback.create", message = "피드백 생성 성공")
    public FeedbackResDTO createFeedback(FeedbackCreateReqDTO req, Long memberId) {
        return FeedbackResDTO.from(feedbackService.createFeedback(req.content(), memberId));
    }

    /** 피드백 목록 조회 */
    @Transactional(readOnly = true)
    public Slice<FeedbackResDTO> getFeedbacks(Pageable pageable) {
        return feedbackService.getFeedbacks(pageable).map(FeedbackResDTO::from);
    }
}
