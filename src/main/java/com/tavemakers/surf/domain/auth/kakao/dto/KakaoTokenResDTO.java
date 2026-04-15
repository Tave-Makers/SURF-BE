package com.tavemakers.surf.domain.auth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
    /**
     * Return a string representation of this DTO with sensitive token fields masked.
     *
     * @return a string representation of this object where `accessToken`, `refreshToken`,
     *         and `idToken` are masked to avoid exposing sensitive values; other fields
     *         (`tokenType`, `expiresIn`, `scope`, `refreshTokenExpiresIn`) are included verbatim.
     */
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

    /**
     * Produces a masked representation of a sensitive string.
     *
     * If the input is null, blank, or shorter than 8 characters, returns "*****".
     * Otherwise returns the first four characters, three dots, and the last four characters.
     *
     * @param s the string to mask
     * @return `"*****"` for null/blank/short inputs; otherwise the string in the form `first4...last4`
     */
    private static String mask(String s) {
        if (s == null || s.isBlank()) return "*****";
        int len = s.length();
        if (len < 8) return "*****";
        return s.substring(0, 4) + "..." + s.substring(len - 4);
    }
}
