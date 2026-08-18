package com.tavemakers.surf.presentation.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신고 대상 이동 정보 DTO")
public record ReportTargetNavigationResDTO(

        @Schema(description = "게시글 ID", example = "12")
        Long postId,

        @Schema(description = "댓글 ID", example = "34")
        Long commentId,

        @Schema(description = "회원 ID", example = "56")
        Long memberId
) {
    /** 게시글 이동 정보를 생성한다. */
    public static ReportTargetNavigationResDTO forPost(Long postId, Long memberId) {
        return new ReportTargetNavigationResDTO(postId, null, memberId);
    }

    /** 댓글 이동 정보를 생성한다. */
    public static ReportTargetNavigationResDTO forComment(Long postId, Long commentId, Long memberId) {
        return new ReportTargetNavigationResDTO(postId, commentId, memberId);
    }

    /** 프로필 이동 정보를 생성한다. */
    public static ReportTargetNavigationResDTO forProfile(Long memberId) {
        return new ReportTargetNavigationResDTO(null, null, memberId);
    }
}
