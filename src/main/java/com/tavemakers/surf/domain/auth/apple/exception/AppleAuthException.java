package com.tavemakers.surf.domain.auth.apple.exception;

import com.tavemakers.surf.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Apple OAuth API 호출 실패 또는 토큰 검증 실패 시 발생하는 예외 */
public class AppleAuthException extends BaseException {

    /** 상태 코드와 메시지로 Apple 인증 예외를 생성한다. */
    public AppleAuthException(HttpStatus status, String message) {
        super(status, message);
    }
}
