package com.tavemakers.surf.domain.auth.apple.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Apple /auth/token 응답 DTO */
public record AppleTokenResDTO(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("id_token")
        String idToken
) {}
