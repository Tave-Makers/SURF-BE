package com.tavemakers.surf.presentation.report.dto.response;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 신고 상세 응답 DTO")
public record AdminReportDetailResDTO(
        @Schema(description = "신고 ID", example = "1")
        Long id,

        @Schema(description = "신고자 ID", example = "10")
        Long reporterMemberId,

        @Schema(description = "신고자 이름", example = "김서퍼")
        String reporterName,

        @Schema(description = "피신고자 ID", example = "20")
        Long reportedMemberId,

        @Schema(description = "피신고자 이름", example = "홍길동")
        String reportedName,

        @Schema(description = "신고 대상 타입", example = "COMMENT")
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "44")
        Long targetId,

        @Schema(description = "신고 사유", example = "SPAM_OR_PROMOTION")
        ReportReasonType reasonType,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        ReportPreviewResDTO preview,

        ReportTargetNavigationResDTO targetNavigation,

        @Schema(description = "처리자 ID", example = "1")
        Long resolvedBy,

        @Schema(description = "처리자 이름", example = "관리자")
        String resolvedByName,

        @Schema(description = "처리 시각", example = "2026-08-16T13:00:00")
        LocalDateTime resolvedAt,

        @Schema(description = "관리자 메모", example = "반복 스팸으로 확인되어 처리했습니다.")
        String adminMemo,

        @Schema(description = "신고 생성 시각", example = "2026-08-16T11:30:00")
        LocalDateTime createdAt
) {
    /** 관리자 상세 응답을 생성한다. */
    public static AdminReportDetailResDTO from(
            Report report,
            String reporterName,
            String reportedName,
            String resolvedByName,
            ReportPreviewResDTO preview,
            ReportTargetNavigationResDTO targetNavigation
    ) {
        return new AdminReportDetailResDTO(
                report.getId(),
                report.getReporterMemberId(),
                reporterName,
                report.getReportedMemberId(),
                reportedName,
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonType(),
                report.getStatus(),
                preview,
                targetNavigation,
                report.getResolvedBy(),
                resolvedByName,
                report.getResolvedAt(),
                report.getAdminMemo(),
                report.getCreatedAt()
        );
    }
}
