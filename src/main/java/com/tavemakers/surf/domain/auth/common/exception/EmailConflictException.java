package com.tavemakers.surf.domain.auth.common.exception;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.global.common.exception.BaseException;
import lombok.Getter;

/**
 * 동일 이메일이 다른 provider로 이미 가입된 경우 발생하는 예외 (D6).
 * existingProvider 를 통해 클라이언트가 안내 분기를 할 수 있다.
 */
@Getter
public class EmailConflictException extends BaseException {

    private final Provider existingProvider;

    /** 충돌한 기존 provider 정보를 담아 예외를 생성한다. */
    public EmailConflictException(Provider existingProvider) {
        super(
                AuthCommonErrorMessage.EMAIL_ALREADY_REGISTERED_OTHER_PROVIDER.getStatus(),
                AuthCommonErrorMessage.EMAIL_ALREADY_REGISTERED_OTHER_PROVIDER.getMessage()
        );
        this.existingProvider = existingProvider;
    }
}
