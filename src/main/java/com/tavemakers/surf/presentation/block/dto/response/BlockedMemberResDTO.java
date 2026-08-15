package com.tavemakers.surf.presentation.block.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 내가 차단한 회원 요약. 등록 응답과 목록 행에 모두 쓴다.
 *
 * <p>탈퇴 회원의 차단 관계는 유지되므로 익명화된 현재 Member 정보가 그대로 내려간다.
 */
@Schema(description = "차단한 회원 정보")
public record BlockedMemberResDTO(

        @Schema(description = "차단당한 회원 ID", example = "12")
        Long memberId,

        @Schema(description = "회원 이름", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.png")
        String profileImageUrl,

        @Schema(description = "차단 일시")
        LocalDateTime blockedAt
) {

    /** 차단 대상 회원과 차단 일시로 응답을 만든다 */
    public static BlockedMemberResDTO of(Member member, LocalDateTime blockedAt) {
        return new BlockedMemberResDTO(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                blockedAt
        );
    }
}
