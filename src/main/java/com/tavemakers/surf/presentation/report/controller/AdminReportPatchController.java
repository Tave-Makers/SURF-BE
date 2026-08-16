package com.tavemakers.surf.presentation.report.controller;

import com.tavemakers.surf.application.report.usecase.AdminReportUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.report.dto.request.ReportStatusPatchReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportDetailResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.report.controller.ResponseMessage.REPORT_STATUS_UPDATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "신고", description = "게시글, 댓글, 프로필에 대한 신고를 접수하고 관리자가 신고 내역을 조회 및 처리할 수 있는 API입니다.")
public class AdminReportPatchController {

    private final AdminReportUsecase adminReportUsecase;

    /** 관리자가 신고 상태를 RESOLVED 또는 REJECTED로 변경한다. */
    @Operation(summary = "신고 상태 변경", description = "관리자가 신고 상태를 RESOLVED 또는 REJECTED로 변경합니다.")
    @PatchMapping("/v1/admin/reports/{reportId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PRESIDENT')")
    public ApiResponse<AdminReportDetailResDTO> patchReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusPatchReqDTO request
    ) {
        Long adminMemberId = SecurityUtils.getCurrentMemberId();
        AdminReportDetailResDTO response = adminReportUsecase.patchReportStatus(reportId, adminMemberId, request);
        return ApiResponse.response(HttpStatus.OK, REPORT_STATUS_UPDATED.getMessage(), response);
    }
}
