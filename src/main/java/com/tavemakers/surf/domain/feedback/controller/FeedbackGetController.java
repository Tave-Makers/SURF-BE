package com.tavemakers.surf.domain.feedback.controller;

import com.tavemakers.surf.domain.feedback.dto.response.FeedbackResDTO;
import com.tavemakers.surf.domain.feedback.usecase.FeedbackUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.feedback.controller.ResponseMessage.FEEDBACK_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "피드백")
public class FeedbackGetController {

    private final FeedbackUsecase feedbackUsecase;

    /** 피드백 조회 (운영진 전용) */
    @Operation(summary = "피드백 조회", description = "운영진이 피드백을 조회합니다.")
    @GetMapping("/v1/admin/feedbacks")
    @PreAuthorize("hasAnyRole('ROOT','MANAGER','PRESIDENT')")
    public ApiResponse<Slice<FeedbackResDTO>> getFeedbacks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Slice<FeedbackResDTO> response = feedbackUsecase.getFeedbacks(pageable);
        return ApiResponse.response(HttpStatus.OK, FEEDBACK_READ.getMessage(), response);
    }
}
