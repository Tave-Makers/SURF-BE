package com.tavemakers.surf.presentation.report.dto.request;

import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "신고 생성 요청 DTO")
public record ReportCreateReqDTO(

        @Schema(description = "신고 대상 타입", example = "POST")
        @NotNull ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "1")
        @NotNull Long targetId,

        @Schema(description = "신고 사유", example = "SPAM_OR_PROMOTION")
        @NotNull ReportReasonType reasonType
) implements LogPropsProvider {

    @Override
    public Map<String, Object> buildProps() {
        return Map.of(
                "target_type", targetType != null ? targetType.name() : "null",
                "target_id", targetId != null ? targetId : -1L,
                "reason_type", reasonType != null ? reasonType.name() : "null"
        );
    }
}
