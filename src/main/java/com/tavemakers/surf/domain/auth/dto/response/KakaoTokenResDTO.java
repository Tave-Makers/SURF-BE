package com.tavemakers.surf.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * Kakao OAuth 토큰 응답 DTO (불변 record 타입)
 * - record의 기본 toString()은 민감값(토큰 등)을 그대로 노출하므로,
 *   toString()을 오버라이드해서 마스킹 처리한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoTokenResDTO(

        String accessToken,
        String tokenType,
        String refreshToken,
        Long expiresIn,
        String scope,
        Long refreshTokenExpiresIn,
        String idToken // OIDC 켰을 때만 채워짐
) {
    @Override
    public String toString() {
        return "KakaoTokenResDTO[" +
                "accessToken=" + mask(accessToken) +
                ", tokenType=" + tokenType +
                ", refreshToken=" + mask(refreshToken) +
                ", expiresIn=" + expiresIn +
                ", scope=" + scope +
                ", refreshTokenExpiresIn=" + refreshTokenExpiresIn +
                ", idToken=" + mask(idToken) +
                ']';
    }

    private static String mask(String s) {
        if (s == null || s.isBlank()) return "*****";
        int len = s.length();
        if (len < 8) return "*****";
        return s.substring(0, 4) + "..." + s.substring(len - 4);
    }
}
