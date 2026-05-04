package com.tavemakers.surf.domain.auth.apple.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Apple OAuth 관련 에러 메시지 */
@Getter
@AllArgsConstructor
public enum AppleAuthErrorMessage {

    IDENTITY_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Apple identityToken 검증에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Apple identityToken이 만료되었습니다."),
    INVALID_ISSUER(HttpStatus.UNAUTHORIZED, "Apple identityToken의 발급자가 올바르지 않습니다."),
    INVALID_AUDIENCE(HttpStatus.UNAUTHORIZED, "Apple identityToken의 audience가 올바르지 않습니다."),
    INVALID_NONCE(HttpStatus.UNAUTHORIZED, "Apple identityToken의 nonce가 올바르지 않습니다."),
    SUBJECT_MISSING(HttpStatus.UNAUTHORIZED, "Apple identityToken에 sub 클레임이 없습니다."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "OAuth state가 유효하지 않습니다."),
    APPLE_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "Apple 토큰 교환에 실패했습니다."),
    APPLE_AUTH_CALLBACK_ERROR(HttpStatus.BAD_REQUEST, "Apple 인증 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
