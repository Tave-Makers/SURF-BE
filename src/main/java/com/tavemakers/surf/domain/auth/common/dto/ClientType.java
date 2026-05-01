package com.tavemakers.surf.domain.auth.common.dto;

/**
 * 로그인/토큰 응답 분기를 위한 클라이언트 타입.
 * <p>{@code X-Client-Type} 요청 헤더가 {@code app}(대소문자 무시)이면 APP, 그 외/누락이면 WEB.
 * <p>Web=쿠키, App=본문 방식으로 RefreshToken 전달 분기에 사용된다.
 */
public enum ClientType {
    WEB, APP;

    /** 헤더 값 → ClientType 매핑. 누락/오타는 안전하게 WEB. */
    public static ClientType from(String header) {
        return "app".equalsIgnoreCase(header) ? APP : WEB;
    }
}
