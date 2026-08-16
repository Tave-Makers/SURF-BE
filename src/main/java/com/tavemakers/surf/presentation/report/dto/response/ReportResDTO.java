package com.tavemakers.surf.presentation.report.dto.response;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "신고 응답 DTO")
public record ReportResDTO(

        @Schema(description = "신고 ID", example = "1")
        Long id,

        @Schema(description = "신고 대상 타입", example = "COMMENT")
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "42")
        Long targetId,

        @Schema(description = "신고 사유", example = "HATE_OR_ABUSE")
        ReportReasonType reasonType,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        ReportPreviewResDTO preview,

        @Schema(description = "신고 생성 시각", example = "2026-08-14T12:00:00")
        LocalDateTime createdAt
) {
    public static ReportResDTO from(Report report, ReportPreviewResDTO preview) {
        return new ReportResDTO(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonType(),
                report.getStatus(),
                preview,
                report.getCreatedAt()
        );
    }
}
