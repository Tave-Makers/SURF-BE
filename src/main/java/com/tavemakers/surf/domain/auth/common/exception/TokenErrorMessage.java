package com.tavemakers.surf.domain.auth.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Refresh Token 관련 공통 에러 메시지
 * - provider 무관 토큰 처리 오류
 */
@Getter
@AllArgsConstructor
public enum TokenErrorMessage {

    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Refresh token이 없습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token이 유효하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "저장된 Refresh token이 없습니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Refresh token 재사용이 감지되었습니다. 보안을 위해 모든 세션이 만료됩니다.");

    private final HttpStatus status;
    private final String message;
}
