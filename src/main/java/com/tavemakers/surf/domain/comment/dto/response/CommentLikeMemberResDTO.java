package com.tavemakers.surf.domain.comment.dto.response;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 좋아요한 회원 조회를 위한 응답 DTO")
public record CommentLikeMemberResDTO(

        @Schema(description = "회원 ID", example = "5")
        Long memberId,

        @Schema(description = "회원 이름", example = "홍길동")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://cdn.surf.com/profile/hong.png")
        String profileImageUrl
) {
    public static CommentLikeMemberResDTO from(Member member) {
        return new CommentLikeMemberResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl()
        );
    }
}
