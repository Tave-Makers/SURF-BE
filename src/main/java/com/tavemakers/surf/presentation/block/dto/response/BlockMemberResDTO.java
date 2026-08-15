package com.tavemakers.surf.presentation.block.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

/** 관리자 목록에서 차단 관계의 양쪽 회원을 표시하기 위한 요약 */
@Schema(description = "차단 관계 참여 회원 요약")
public record BlockMemberResDTO(

        @Schema(description = "회원 ID", example = "12")
        Long memberId,

        @Schema(description = "회원 이름", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.png")
        String profileImageUrl
) {

    /** 회원 엔티티에서 표시용 요약만 추린다 */
    public static BlockMemberResDTO from(Member member) {
        return new BlockMemberResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl()
        );
    }
}
