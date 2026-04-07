package com.tavemakers.surf.domain.feedback.usecase;

import com.tavemakers.surf.domain.feedback.dto.request.FeedbackCreateReqDTO;
import com.tavemakers.surf.domain.feedback.dto.response.FeedbackResDTO;
import com.tavemakers.surf.domain.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 피드백 Usecase */
@Service
@RequiredArgsConstructor
public class FeedbackUsecase {

    private final FeedbackService feedbackService;

    /** 피드백 생성 */
    @Transactional
    public FeedbackResDTO createFeedback(FeedbackCreateReqDTO req, Long memberId) {
        return feedbackService.createFeedback(req, memberId);
    }

    /** 피드백 목록 조회 */
    @Transactional(readOnly = true)
    public Slice<FeedbackResDTO> getFeedbacks(Pageable pageable) {
        return feedbackService.getFeedbacks(pageable);
    }
}
