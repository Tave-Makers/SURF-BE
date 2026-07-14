package com.tavemakers.surf.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class JwtService {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String ROLE_PREFIX = "ROLE_";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpireMs;

    // 쿠키 속성은 프로필명이 아닌 배포 토폴로지(프론트↔API 도메인 관계) 기준으로 결정한다.
    // 크로스 사이트 배포면 SameSite=None + Secure 필수, 같은 등록 도메인 공유 시에만 domain 지정.
    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:None}")
    private String cookieSameSite;

    @Value("${jwt.cookie.domain:}")
    private String cookieDomain;

    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Access Token 생성 */
    public String createAccessToken(Long memberId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("role", ROLE_PREFIX+role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpireMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Refresh Token 생성 */
    public String createRefreshToken(Long memberId, String deviceId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("deviceId", deviceId)
                .claim("jti", UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpireMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 요청에서 Refresh Token 추출 */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie c : cookies) {
            if (REFRESH_COOKIE_NAME.equals(c.getName())) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }

    /** 토큰에서 회원 ID 추출 */
    public Optional<Long> extractMemberId(String token) {
        try {
            Claims claims = parseClaims(token);
            String sub = claims.getSubject();
            return Optional.of(Long.parseLong(sub));
        } catch (JwtException | NumberFormatException e) {
            log.error("토큰에서 memberId 추출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 토큰 유효성 검증 */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    /** 토큰 만료 시간 조회 */
    public long getExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().getTime();
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("토큰 만료 시간 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    /** 인증용 쿠키 공통 빌더 — refresh·deviceId 쿠키가 동일한 토폴로지 속성을 공유한다 */
    public ResponseCookie buildAuthCookie(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(maxAge)
                        .secure(cookieSecure)
                        .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    /** Refresh Token 쿠키 생성 */
    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return buildAuthCookie(REFRESH_COOKIE_NAME, refreshToken, Duration.ofMillis(refreshTokenExpireMs));
    }

    /** Refresh Token 쿠키 전송 */
    public void sendRefreshToken(HttpServletResponse res, String refreshToken) {
        ResponseCookie refreshCookie = buildRefreshTokenCookie(refreshToken);
        res.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 요청 헤더에서 Access Token 추출 */
    public Optional<String> extractAccessTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }

        if (header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }

        return Optional.of(header);
    }

    /** 토큰에서 Device ID 추출 */
    public Optional<String> extractDeviceId(String token) {
        try {
            Claims claims = parseClaims(token);
            return Optional.ofNullable(claims.get("deviceId", String.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Refresh Token 쿠키 삭제 */
    public void clearRefreshTokenCookie(HttpServletResponse res) {
        ResponseCookie refreshCookie = buildAuthCookie(REFRESH_COOKIE_NAME, "", Duration.ZERO);
        res.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
