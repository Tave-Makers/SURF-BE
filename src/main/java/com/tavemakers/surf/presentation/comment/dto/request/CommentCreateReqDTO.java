package com.tavemakers.surf.presentation.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "댓글 생성 DTO")
public record CommentCreateReqDTO(

        @Schema(
                description = """
                부모 댓글 ID (바로 위 상위 댓글).
                자동 멘션(=대댓글)일 때만 값 있음.
                수동 멘션 또는 일반 루트 댓글이면 null.
                """,
                example = "null"
        )
        Long parentId,

        @Schema(description = "내용", example = "좋은 공지 감사합니다!")
        @NotBlank
        @Size(max = 1000, message = "댓글은 1000자 이하만 가능합니다.")
        String content,

        @Schema(description = "멘션할 회원 ID 목록", example = "[5]")
        List<Long> mentionMemberIds
) {}