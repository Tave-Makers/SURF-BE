package com.tavemakers.surf.domain.comment.presentation.dto.response;

import com.tavemakers.surf.domain.comment.domain.entity.CommentMention;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 내 멘션 표기를 위한 응답 DTO")
public record MentionResDTO(
        @Schema(description = "멘션된 회원 ID", example = "5")
        Long memberId,

        @Schema(description = "멘션된 회원 이름", example = "홍길동")
        String nickname
) {
    public static MentionResDTO from(CommentMention mention) {
        return new MentionResDTO(
                mention.getMentionedMember().getId(),
                mention.getMentionedMember().getName()
        );
    }
}
