package com.tavemakers.surf.domain.auth.kakao.exception;

import com.tavemakers.surf.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** 카카오 OAuth API 호출 실패 시 발생하는 예외 */
public class KakaoAuthException extends BaseException {

    /** 상태 코드와 메시지로 카카오 인증 예외를 생성한다. */
    public KakaoAuthException(HttpStatus status, String message) {
        super(status, message);
    }
}
