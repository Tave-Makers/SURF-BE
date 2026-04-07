package com.tavemakers.surf.domain.feedback.controller;

import com.tavemakers.surf.domain.feedback.dto.request.FeedbackCreateReqDTO;
import com.tavemakers.surf.domain.feedback.dto.response.FeedbackResDTO;
import com.tavemakers.surf.domain.feedback.usecase.FeedbackUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.feedback.controller.ResponseMessage.FEEDBACK_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "피드백")
public class FeedbackCreateController {

    private final FeedbackUsecase feedbackUsecase;

    /** 피드백 생성 (로그인 사용자) */
    @Operation(summary = "피드백 생성", description = "익명의 피드백을 생성합니다. (하루 3회 제한)")
    @PostMapping("/v1/user/feedbacks")
    public ApiResponse<FeedbackResDTO> createFeedback(
            @Valid @RequestBody FeedbackCreateReqDTO req
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        FeedbackResDTO response = feedbackUsecase.createFeedback(req, memberId);
        return ApiResponse.response(HttpStatus.CREATED, FEEDBACK_CREATED.getMessage(), response);
    }
}
