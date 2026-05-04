package com.tavemakers.surf.domain.auth.apple.service;

import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import org.springframework.stereotype.Service;

/** Apple 로그인 이벤트 로깅 서비스 */
@Service
public class AppleAuthLogService {

    /** Apple 인가 요청 로그 */
    @LogEvent("login.apple.request")
    public void logAuthorize(
            @LogParam("login_method") String loginMethod,
            @LogParam("redirect_uri") String redirectUri
    ) {}

    /** Apple 콜백 로그 */
    @LogEvent("login.apple.callback")
    public void logCallback(
            @LogParam("provider") String provider
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
    ) {}
}
