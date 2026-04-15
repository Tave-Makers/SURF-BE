package com.tavemakers.surf.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorMessage {
    KAKAO_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 교환에 실패 했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다."),
    KAKAO_TOKEN_VALIDATE_FAILED(HttpStatus.UNAUTHORIZED, "카카오 AccessToken 검증에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
