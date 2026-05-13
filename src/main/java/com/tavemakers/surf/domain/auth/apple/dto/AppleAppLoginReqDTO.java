package com.tavemakers.surf.domain.auth.apple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Apple SDK 앱 로그인 요청 DTO */
@Schema(description = "Apple SDK 앱 로그인 요청")
public record AppleAppLoginReqDTO(

        @NotBlank
        @Schema(description = "Apple SDK가 발급한 identityToken (RS256 JWT)")
        String identityToken,

        @NotBlank
        @Schema(description = "클라이언트가 생성한 nonce 원문. Apple idToken의 nonce claim = SHA-256(이 값).")
        String nonce,

        @Schema(description = "사용자 이름 — Apple SDK 최초 로그인 시에만 제공. 이후 null.")
        String name,

        @Schema(description = "Apple SDK 인가 코드 — 탈퇴 시 /auth/revoke 호출을 위해 refresh_token 교환에 사용. 미전달 시 탈퇴 revoke 불가. 매 로그인 시 전달 권장.")
        String authorizationCode
) {}
