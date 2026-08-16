package com.tavemakers.surf.presentation.report.controller;

import com.tavemakers.surf.application.report.usecase.AdminReportUsecase;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.report.dto.request.ReportStatusUpdateReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportDetailResDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportSliceResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.report.controller.ResponseMessage.REPORT_DETAIL_READ;
import static com.tavemakers.surf.presentation.report.controller.ResponseMessage.REPORT_LIST_READ;
import static com.tavemakers.surf.presentation.report.controller.ResponseMessage.REPORT_STATUS_UPDATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "신고", description = "사용자의 신고 접수/관리자의 신고 내역 조회 및 처리 관련 API")
public class AdminReportController {

    private final AdminReportUsecase adminReportUsecase;

    @Operation(summary = "신고 목록 조회", description = "관리자가 신고 목록을 최신순으로 조회합니다.")
    @GetMapping("/v1/admin/reports")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PRESIDENT')")
    public ApiResponse<AdminReportSliceResDTO> getReports(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        AdminReportSliceResDTO response = adminReportUsecase.getReports(targetType, status, pageable);
        return ApiResponse.response(HttpStatus.OK, REPORT_LIST_READ.getMessage(), response);
    }

    @Operation(summary = "신고 상세 조회", description = "관리자가 특정 신고의 상세 정보를 조회합니다.")
    @GetMapping("/v1/admin/reports/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PRESIDENT')")
    public ApiResponse<AdminReportDetailResDTO> getReport(@PathVariable Long reportId) {
        AdminReportDetailResDTO response = adminReportUsecase.getReport(reportId);
        return ApiResponse.response(HttpStatus.OK, REPORT_DETAIL_READ.getMessage(), response);
    }

    @Operation(summary = "신고 상태 변경", description = "관리자가 신고 상태를 RESOLVED 또는 REJECTED로 변경합니다.")
    @PatchMapping("/v1/admin/reports/{reportId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PRESIDENT')")
    public ApiResponse<AdminReportDetailResDTO> updateStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateReqDTO request
    ) {
        Long adminMemberId = SecurityUtils.getCurrentMemberId();
        AdminReportDetailResDTO response = adminReportUsecase.updateStatus(reportId, adminMemberId, request);
        return ApiResponse.response(HttpStatus.OK, REPORT_STATUS_UPDATED.getMessage(), response);
    }
}
