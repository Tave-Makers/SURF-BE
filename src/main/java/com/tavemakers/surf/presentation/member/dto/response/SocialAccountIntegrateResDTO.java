package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.auth.common.enums.Provider;

/** 소셜 계정 통합 결과 — 기존 회원에 연결된 provider. */
public record SocialAccountIntegrateResDTO(
        String provider
) {
    public static SocialAccountIntegrateResDTO from(Provider provider) {
        return new SocialAccountIntegrateResDTO(provider.name());
    }
}
