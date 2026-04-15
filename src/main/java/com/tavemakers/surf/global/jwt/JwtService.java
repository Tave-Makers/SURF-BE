package com.tavemakers.surf.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
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
    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpireMs;

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

    private boolean isDev() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    /**
     * Checks whether the application is running with the "test" Spring profile active.
     *
     * @return `true` if the "test" profile is active, `false` otherwise.
     */
    private boolean isTest() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    /**
     * Build a refresh-token cookie configured for the current environment.
     *
     * The cookie is HTTP-only, scoped to path "/", and has a max age equal to the refresh-token expiration.
     * In the "dev" profile the cookie is marked secure, SameSite=None, and set for domain ".tavesurf.site";
     * in the "test" profile it is SameSite=None and not secure; otherwise it is SameSite=Lax and not secure.
     *
     * @param refreshToken the refresh token value to store in the cookie
     * @return the configured ResponseCookie for the refresh token
     */
    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofMillis(refreshTokenExpireMs));

        if (isDev()) {
            builder.secure(true).sameSite("None").domain(".tavesurf.site");
        } else if (isTest()) {
            builder.secure(false).sameSite("None");
        } else {
            builder.secure(false).sameSite("Lax");
        }

        return builder.build();
    }

    /**
     * Adds a refresh-token cookie to the given HTTP response.
     *
     * The cookie is built with environment-dependent attributes (e.g., HttpOnly, path, max-age,
     * and profile-specific secure/sameSite/domain settings) and added to the response's
     * Set-Cookie header.
     *
     * @param res the HTTP response to which the refresh-token cookie will be added
     * @param refreshToken the refresh token value to store in the cookie
     */
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
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ZERO);

        if (isDev()) {
            builder
                    .secure(true)
                    .domain(".tavesurf.site")
                    .sameSite("None");
        } else if (isTest()) {
            builder
                    .secure(false)
                    .sameSite("None");
        }

        ResponseCookie refreshCookie = builder.build();
        res.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
