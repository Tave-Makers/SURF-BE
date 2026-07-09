package com.tavemakers.surf.domain.auth.kakao.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 카카오 OAuth API 관련 에러 메시지
 */
@Getter
@AllArgsConstructor
public enum KakaoAuthErrorMessage {

    KAKAO_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 교환에 실패했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다."),
    KAKAO_TOKEN_VALIDATE_FAILED(HttpStatus.UNAUTHORIZED, "카카오 AccessToken 검증에 실패했습니다."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "OAuth state가 유효하지 않습니다."),
    KAKAO_AUTH_CALLBACK_ERROR(HttpStatus.BAD_REQUEST, "카카오 인증 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
