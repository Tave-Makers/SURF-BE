package com.tavemakers.surf.presentation.report.dto.response;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 신고 목록 아이템 DTO")
public record AdminReportListItemResDTO(
        @Schema(description = "신고 ID", example = "1")
        Long id,

        @Schema(description = "신고 대상 타입", example = "POST")
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "42")
        Long targetId,

        @Schema(description = "신고 사유", example = "SPAM_OR_PROMOTION")
        ReportReasonType reasonType,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        @Schema(description = "신고자 이름", example = "김서퍼")
        String reporterName,

        @Schema(description = "피신고자 이름", example = "홍길동")
        String reportedName,

        ReportPreviewResDTO preview,

        @Schema(description = "신고 생성 시각", example = "2026-08-16T11:30:00")
        LocalDateTime createdAt
) {
    /** 관리자 목록 응답을 생성한다. */
    public static AdminReportListItemResDTO of(
            Report report,
            String reporterName,
            String reportedName,
            ReportPreviewResDTO preview
    ) {
        return new AdminReportListItemResDTO(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonType(),
                report.getStatus(),
                reporterName,
                reportedName,
                preview,
                report.getCreatedAt()
        );
    }
}
