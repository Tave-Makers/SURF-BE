package com.tavemakers.surf.domain.feedback.facade;

import com.tavemakers.surf.domain.feedback.dto.req.FeedbackCreateReqDTO;
import com.tavemakers.surf.domain.feedback.dto.res.FeedbackResDTO;
import com.tavemakers.surf.domain.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackFacade {
    private final FeedbackService feedbackService;

    public FeedbackResDTO createFeedback(FeedbackCreateReqDTO req, Long memberId) {
        return feedbackService.createFeedback(req, memberId);
    }

    public Slice<FeedbackResDTO> getFeedbacks(Pageable pageable) {
        return feedbackService.getFeedbacks(pageable);
    }
}
