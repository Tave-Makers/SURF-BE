package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 계정 통합 대상 확인에 필요한 최소 정보를 반환한다. */
@Schema(description = "계정 통합 대상 확인 응답 DTO")
public record IntegrationTargetResDTO(

        @Schema(description = "통합 대상에 연결된 소셜 목록 — 이 값으로 로그인 버튼을 렌더링한다", example = "[\"KAKAO\"]")
        List<String> providers,

        @Schema(description = "온보딩에 입력했던 이메일", example = "hong@example.com")
        String email,

        @Schema(description = "온보딩에 입력했던 전화번호", example = "01012345678")
        String phoneNumber,

        @Schema(description = "통합 대상 이름", example = "홍길동")
        String username,

        @Schema(description = "통합 대상 프로필 이미지 URL")
        String profileImageUrl
) {

    /** 통합 대기 정보와 대상 회원으로 응답을 생성한다. */
    public static IntegrationTargetResDTO of(
            PendingSocialIntegration pending,
            Member target,
            List<SocialAccount> targetSocialAccounts
    ) {
        List<String> providers = targetSocialAccounts.stream()
                .map(socialAccount -> socialAccount.getProvider().name())
                .toList();

        return new IntegrationTargetResDTO(
                providers,
                pending.getNormalizedEmail(),
                pending.getNormalizedPhone(),
                target.getName(),
                target.getProfileImageUrl()
        );
    }
}
