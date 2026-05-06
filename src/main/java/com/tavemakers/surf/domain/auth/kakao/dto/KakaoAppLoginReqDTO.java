package com.tavemakers.surf.domain.auth.kakao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 카카오 SDK 앱 로그인 요청 DTO */
@Schema(description = "카카오 SDK 앱 로그인 요청")
public record KakaoAppLoginReqDTO(

        @NotBlank
        @Schema(description = "카카오 SDK가 발급한 AccessToken")
        String accessToken
) {}
