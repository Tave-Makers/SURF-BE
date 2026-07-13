package com.tavemakers.surf.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "surf-test-jwt-secret-key-0123456789012345678901234567890123456789";
    private static final String OTHER_SECRET = "surf-forged-jwt-secret-key-9876543210987654321098765432109876543210";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new MockEnvironment());
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpireMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpireMs", 3_600_000L);
        jwtService.init();
    }

    @Test
    @DisplayName("만료된 토큰이면 extractMemberId는 예외 없이 Optional.empty()를 반환한다")
    void extractMemberId_expiredToken_returnsEmpty() {
        String expiredToken = buildToken(SECRET, new Date(System.currentTimeMillis() - 1_000));

        assertThat(jwtService.extractMemberId(expiredToken)).isEmpty();
    }

    @Test
    @DisplayName("서명이 일치하지 않는 토큰이면 extractMemberId는 예외 없이 Optional.empty()를 반환한다")
    void extractMemberId_forgedSignature_returnsEmpty() {
        String forgedToken = buildToken(OTHER_SECRET, new Date(System.currentTimeMillis() + 3_600_000));

        assertThat(jwtService.extractMemberId(forgedToken)).isEmpty();
    }

    @Test
    @DisplayName("유효한 토큰이면 extractMemberId는 memberId를 반환한다")
    void extractMemberId_validToken_returnsMemberId() {
        String validToken = jwtService.createAccessToken(42L, "MEMBER");

        assertThat(jwtService.extractMemberId(validToken)).contains(42L);
    }

    private String buildToken(String secret, Date expiration) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject("1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
