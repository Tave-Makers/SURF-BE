package com.tavemakers.surf.presentation.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 소셜 계정 통합 요청 — 온보딩 case B에서 발급받은 1회성 통합 토큰 (§3.6.2 c). */
public record SocialAccountIntegrateReqDTO(
        @NotBlank String integrationToken
) {}
