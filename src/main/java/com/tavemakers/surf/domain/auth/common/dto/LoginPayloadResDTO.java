package com.tavemakers.surf.domain.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

/**
 * 클라이언트 타입별 로그인 응답 wrapper (D2).
 * <ul>
 *   <li>WEB: refreshCookie=set → 컨트롤러가 {@code Set-Cookie} 헤더로 부착</li>
 *   <li>APP: loginRes 안에 refreshToken 포함 → 컨트롤러가 loginRes 그대로 반환</li>
 * </ul>
 * <p>Usecase ↔ Controller 간 내부 전달 객체. 외부 응답 본문은 컨트롤러가 {@code loginRes()}로 조립한다.
 */
@Schema(description = "클라이언트 타입별 로그인 응답 wrapper (내부 전달용)")
public record LoginPayloadResDTO(

        LoginResDTO loginRes,

        @JsonIgnore
        @Schema(hidden = true)
        ResponseCookie refreshCookie,

        @JsonIgnore
        @Schema(hidden = true)
        ResponseCookie deviceIdCookie
) {

    /** WEB 클라이언트용 — RefreshToken + deviceId 모두 쿠키로 전달. */
    public static LoginPayloadResDTO web(LoginResDTO loginRes, ResponseCookie refreshCookie, ResponseCookie deviceIdCookie) {
        return new LoginPayloadResDTO(loginRes, refreshCookie, deviceIdCookie);
    }

    /** APP 클라이언트용 — RefreshToken 은 loginRes 안에 포함. */
    public static LoginPayloadResDTO app(LoginResDTO loginRes) {
        return new LoginPayloadResDTO(loginRes, null, null);
    }

    /** WEB 응답 빌더 — refreshCookie + deviceIdCookie(신규 시)를 SET_COOKIE 헤더에 조립. */
    public ResponseEntity.BodyBuilder toWebResponseBuilder() {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        if (deviceIdCookie != null) {
            builder.header(HttpHeaders.SET_COOKIE, deviceIdCookie.toString());
        }
        return builder;
    }
}
