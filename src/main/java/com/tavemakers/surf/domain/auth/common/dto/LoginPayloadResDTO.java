package com.tavemakers.surf.domain.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseCookie;

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
        ResponseCookie refreshCookie
) {

    /** WEB 클라이언트용 — RefreshToken 은 쿠키로 전달. */
    public static LoginPayloadResDTO web(LoginResDTO loginRes, ResponseCookie refreshCookie) {
        return new LoginPayloadResDTO(loginRes, refreshCookie);
    }

    /** APP 클라이언트용 — RefreshToken 은 loginRes 안에 포함. */
    public static LoginPayloadResDTO app(LoginResDTO loginRes) {
        return new LoginPayloadResDTO(loginRes, null);
    }
}
