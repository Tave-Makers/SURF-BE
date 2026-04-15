package com.tavemakers.surf.domain.auth.service;

import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import org.springframework.stereotype.Service;

@Service
public class KakaoAuthLogService {

    /** 카카오 인가 요청 로그 */
    @LogEvent("login.kakao.request")
    public void logAuthorize(
            @LogParam("login_method") String loginMethod,
            @LogParam("redirect_uri") String redirectUri
    ) {}

    /** 카카오 콜백 로그 */
    @LogEvent("login.kakao.callback")
    public void logCallback(
            @LogParam("provider") String provider,
            @LogParam("code_length") int codeLength
    ) {}

    /** 로그인 성공 로그 */
    @LogEvent("login.succeeded")
    public void logLoginSuccess(
            @LogParam("user_id") Long userId,
            @LogParam("issued_token") String issuedToken
    ) {}

    /** 로그인 실패 로그 */
    @LogEvent("login.failed")
    public void logLoginFailed(
            @LogParam("error_code") int errorCode,
            @LogParam("error_msg") String errorMsg
    ) {
        throw new IllegalStateException(errorMsg);
    }
}
