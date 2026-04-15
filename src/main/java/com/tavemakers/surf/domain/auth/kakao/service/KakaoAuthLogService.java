package com.tavemakers.surf.domain.auth.kakao.service;

import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import org.springframework.stereotype.Service;

@Service
public class KakaoAuthLogService {

    /**
     * Record a Kakao authorization request event.
     *
     * @param loginMethod the authentication method used for the request (logged as `login_method`)
     * @param redirectUri the redirect URI included in the authorization request (logged as `redirect_uri`)
     */
    @LogEvent("login.kakao.request")
    public void logAuthorize(
            @LogParam("login_method") String loginMethod,
            @LogParam("redirect_uri") String redirectUri
    ) {}

    /**
     * Record a Kakao callback log for the OAuth flow.
     *
     * @param provider  the OAuth provider identifier (for example, "kakao")
     * @param codeLength the length of the authorization code received
     */
    @LogEvent("login.kakao.callback")
    public void logCallback(
            @LogParam("provider") String provider,
            @LogParam("code_length") int codeLength
    ) {}

    /**
     * Record a successful login event for a user.
     *
     * @param userId      the identifier of the user who logged in
     * @param issuedToken the authentication token issued to the user
     */
    @LogEvent("login.succeeded")
    public void logLoginSuccess(
            @LogParam("user_id") Long userId,
            @LogParam("issued_token") String issuedToken
    ) {}

    /**
     * Record a login failure event and then terminate the flow by throwing an exception.
     *
     * @param errorCode numeric code representing the failure reason
     * @param errorMsg  human-readable message describing the failure; used as the thrown exception message
     * @throws IllegalStateException always thrown with {@code errorMsg}
     */
    @LogEvent("login.failed")
    public void logLoginFailed(
            @LogParam("error_code") int errorCode,
            @LogParam("error_msg") String errorMsg
    ) {
        throw new IllegalStateException(errorMsg);
    }
}
