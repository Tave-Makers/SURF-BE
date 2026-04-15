package com.tavemakers.surf.domain.auth.dto.response;

import org.springframework.http.ResponseCookie;

/**
 * 카카오 로그인 Usecase 처리 결과
 * - loginRes: 프론트엔드로 전달할 사용자 정보 및 AccessToken
 * - refreshCookie: HttpOnly 쿠키로 전달할 RefreshToken
 */
public record KakaoLoginResult(
        LoginResDTO loginRes,
        ResponseCookie refreshCookie
) {}
