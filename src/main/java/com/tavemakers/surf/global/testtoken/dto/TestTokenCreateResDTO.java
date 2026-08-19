package com.tavemakers.surf.global.testtoken.dto;

/** 테스트 전용 토큰 발급 응답 (액세스 토큰) */
public record TestTokenCreateResDTO(String accessToken) {

    public static TestTokenCreateResDTO from(String accessToken) {
        return new TestTokenCreateResDTO(accessToken);
    }
}
