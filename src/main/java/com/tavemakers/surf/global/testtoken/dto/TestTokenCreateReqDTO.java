package com.tavemakers.surf.global.testtoken.dto;

import jakarta.validation.constraints.NotNull;

/** 테스트 전용 토큰 발급 요청 (대상 회원 ID) */
public record TestTokenCreateReqDTO(@NotNull Long memberId) {
}
