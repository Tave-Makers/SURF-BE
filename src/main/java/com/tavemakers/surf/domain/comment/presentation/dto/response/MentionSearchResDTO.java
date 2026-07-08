package com.tavemakers.surf.domain.comment.presentation.dto.response;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멘션 검색 시 반환되는 회원 정보 응답 DTO")
public record MentionSearchResDTO(

        @Schema(description = "회원 ID", example = "2")
        Long memberId,

        @Schema(description = "회원 이름(닉네임)", example = "홍길동")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://cdn.surf.com/profile/hong.png")
        String profileImageUrl,

        @Schema(description = "최초 활동 기수", example = "13")
        Integer firstGeneration
) {
    public static MentionSearchResDTO from(Member member) {
        return new MentionSearchResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                member.getFirstGeneration()
        );
    }
}
