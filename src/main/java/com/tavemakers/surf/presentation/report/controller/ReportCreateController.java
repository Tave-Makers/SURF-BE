package com.tavemakers.surf.presentation.report.controller;

import com.tavemakers.surf.application.report.usecase.ReportUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.report.dto.request.ReportCreateReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.report.controller.ResponseMessage.REPORT_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "신고", description = "게시글/댓글/프로필 신고 API")
public class ReportCreateController {

    private final ReportUsecase reportUsecase;

    @Operation(summary = "신고 접수", description = "게시글, 댓글, 프로필 신고를 접수합니다.")
    @PostMapping("/v1/user/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportResDTO> createReport(@Valid @RequestBody ReportCreateReqDTO request) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        ReportResDTO response = reportUsecase.createReport(memberId, request);
        return ApiResponse.response(HttpStatus.CREATED, REPORT_CREATED.getMessage(), response);
    }
}
