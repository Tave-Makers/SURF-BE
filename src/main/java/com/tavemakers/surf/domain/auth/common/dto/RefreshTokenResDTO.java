package com.tavemakers.surf.domain.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Access Token 재발급 응답 DTO.
 * <ul>
 *   <li>WEB: accessToken 만 본문, refreshToken 은 쿠키로 회전됨 → null 직렬화 제외</li>
 *   <li>APP: accessToken + refreshToken 모두 본문 (헤더 입력, 본문 출력)</li>
 * </ul>
 */
@Schema(description = "Access Token 재발급 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefreshTokenResDTO(

        @Schema(description = "재발급된 JWT Access Token")
        String accessToken,

        @Schema(description = "APP 클라이언트일 때만 포함. WEB 은 쿠키로 회전.")
        String refreshToken
) {

    /** WEB 응답 — RefreshToken 은 쿠키로 회전되었으므로 본문에 미포함. */
    public static RefreshTokenResDTO web(String accessToken) {
        return new RefreshTokenResDTO(accessToken, null);
    }

    /** APP 응답 — 새 RefreshToken 도 본문에 포함. */
    public static RefreshTokenResDTO app(String accessToken, String refreshToken) {
        return new RefreshTokenResDTO(accessToken, refreshToken);
    }
}
