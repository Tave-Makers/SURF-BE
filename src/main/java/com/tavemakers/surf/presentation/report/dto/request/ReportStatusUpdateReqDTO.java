package com.tavemakers.surf.presentation.report.dto.request;

import com.tavemakers.surf.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신고 상태 변경 요청 DTO")
public record ReportStatusUpdateReqDTO(

        @NotNull(message = "status는 필수입니다.")
        @Schema(description = "변경할 신고 상태", example = "RESOLVED")
        ReportStatus status,

        @Size(max = 1000, message = "adminMemo는 1000자 이하로 입력해주세요.")
        @Schema(description = "관리자 메모", example = "스팸 게시글 확인 후 처리했습니다.")
        String adminMemo
) {
}
