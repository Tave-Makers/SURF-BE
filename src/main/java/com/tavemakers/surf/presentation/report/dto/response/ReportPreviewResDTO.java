package com.tavemakers.surf.presentation.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신고 대상 preview DTO")
public record ReportPreviewResDTO(

        @Schema(description = "작성자 이름", example = "홍길동")
        String writerName,

        @Schema(description = "게시글 제목", example = "전반기 시상식 안내")
        String title,

        @Schema(description = "댓글 내용", example = "안녕하세요. 회차 최쿠루입니다.")
        String content,

        @Schema(description = "프로필 대상 멤버명", example = "홍길동")
        String memberName
) {
    public static ReportPreviewResDTO forPost(String writerName, String title) {
        return new ReportPreviewResDTO(writerName, title, null, null);
    }

    public static ReportPreviewResDTO forComment(String writerName, String content) {
        return new ReportPreviewResDTO(writerName, null, content, null);
    }

    public static ReportPreviewResDTO forProfile(String memberName) {
        return new ReportPreviewResDTO(null, null, null, memberName);
    }
}
