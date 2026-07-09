
package com.tavemakers.surf.presentation.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "댓글 목록 + 총 개수 응답 DTO")
public record CommentListResDTO(

        @Schema(description = "댓글 목록")
        List<CommentResDTO> comments,

        @Schema(description = "총 댓글 수", example = "37")
        long totalCount,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {}
