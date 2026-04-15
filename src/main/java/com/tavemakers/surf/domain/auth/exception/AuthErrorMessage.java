package com.tavemakers.surf.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorMessage {

    // [카카오 로그인 관련 오류]
    KAKAO_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 교환에 실패 했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다."),
    KAKAO_TOKEN_VALIDATE_FAILED(HttpStatus.UNAUTHORIZED, "카카오 AccessToken 검증에 실패했습니다."),

    // [Refresh Token 관련 오류]
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Refresh token이 없습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token이 유효하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "저장된 Refresh token이 없습니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Refresh token 재사용이 감지되었습니다. 보안을 위해 모든 세션이 만료됩니다.");

    private final HttpStatus status;
    private final String message;
}
