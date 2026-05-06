package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.common.dto.LoginResDTO;
import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 로그인 성공 후 SURF JWT 발급 + 응답 wrapper 조립 일원화.
 * <p>4조합(Kakao/Apple × Web/App) 모두 본 클래스를 경유한다. Usecase 는 ClientType 만 넘기고 분기 로직을 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class LoginTokenIssuer {

    private static final String DEVICE_ID_HEADER = "X-Device-Id";
    private static final String DEVICE_ID_COOKIE = "__surf_device_id";

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /** 회원 + OAuth 사용자 정보 + 클라이언트 타입 + 요청 컨텍스트 → 토큰 발급 후 응답 wrapper 반환. */
    public LoginPayloadResDTO issue(Member member, OAuthUserInfoDTO info, ClientType clientType, HttpServletRequest request) {
        DeviceResolution resolution = resolveDeviceId(request, clientType);
        // 동일 deviceId의 기존 세션 선제 폐기 — 한 기기 한 세션 보장
        refreshTokenService.invalidate(member.getId(), resolution.deviceId());

        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());

        if (clientType == ClientType.APP) {
            String refreshToken = refreshTokenService.issueRaw(member.getId(), resolution.deviceId());
            LoginResDTO loginRes = LoginResDTO.ofApp(info.nickname(), info.email(), accessToken, refreshToken, info.profileImageUrl());
            return LoginPayloadResDTO.app(loginRes);
        }
        LoginResDTO loginRes = LoginResDTO.of(info.nickname(), info.email(), accessToken, info.profileImageUrl());
        ResponseCookie rtCookie = refreshTokenService.issue(member.getId(), resolution.deviceId());
        return LoginPayloadResDTO.web(loginRes, rtCookie, resolution.newCookie());
    }

    /**
     * 클라이언트 타입별 deviceId 결정.
     * <ul>
     *   <li>APP: {@code X-Device-Id} 헤더 → fallback UUID (쿠키 없음)</li>
     *   <li>WEB: {@code __surf_device_id} 쿠키 존재 → 재사용 (newCookie=null) / 없음 → UUID 생성 + 쿠키 발급</li>
     * </ul>
     */
    private DeviceResolution resolveDeviceId(HttpServletRequest request, ClientType clientType) {
        if (clientType == ClientType.APP) {
            String header = request.getHeader(DEVICE_ID_HEADER);
            String deviceId = (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
            return new DeviceResolution(deviceId, null);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (DEVICE_ID_COOKIE.equals(cookie.getName())) {
                    return new DeviceResolution(cookie.getValue(), null);
                }
            }
        }
        // WEB 첫 로그인 — UUID 생성 후 브라우저에 저장할 쿠키도 함께 반환
        String newId = UUID.randomUUID().toString();
        ResponseCookie newCookie = ResponseCookie.from(DEVICE_ID_COOKIE, newId)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")
                .build();
        return new DeviceResolution(newId, newCookie);
    }

    private record DeviceResolution(String deviceId, ResponseCookie newCookie) {}
}
