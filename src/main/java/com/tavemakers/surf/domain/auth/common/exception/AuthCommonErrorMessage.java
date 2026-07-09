package com.tavemakers.surf.domain.auth.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 인증 공통 에러 메시지 — provider 공통으로 사용한다. */
@Getter
@AllArgsConstructor
public enum AuthCommonErrorMessage {

    EMAIL_ALREADY_REGISTERED_OTHER_PROVIDER(
            HttpStatus.CONFLICT,
            "이미 다른 방식으로 가입된 이메일입니다. 기존 로그인 방식을 이용해 주세요."
    );

    private final HttpStatus status;
    private final String message;
}
