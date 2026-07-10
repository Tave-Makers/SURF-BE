package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testcontainers 기반 통합 테스트(`RefreshTokenServiceTest`)로는 재현이 까다로운
 * 인프라 예외 경로(invalidateAll 실패, Lua null 반환)를 Mockito로 검증한다.
 */
@DisplayName("RefreshTokenService — 인프라 예외 처리 단위 테스트")
class RefreshTokenServiceUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final String DEVICE_ID = "device-A";
    private static final String REFRESH_TOKEN = "header.payload.signature";

    private JwtService jwtService;
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractMemberId(REFRESH_TOKEN)).thenReturn(Optional.of(MEMBER_ID));
        when(jwtService.extractDeviceId(REFRESH_TOKEN)).thenReturn(Optional.of(DEVICE_ID));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenService = new RefreshTokenService(jwtService, redisTemplate);
    }

    @Test
    @DisplayName("invalidateAll이 Redis 장애로 실패해도 REUSE_DETECTED 401은 반드시 던진다")
    @SuppressWarnings("unchecked")
    void reuseDetected_isThrownEvenWhenInvalidateAllFails() {
        // CAS → MISMATCH (저장값과 다름) — 진짜 재사용 경로
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(0L);
        // grace 키는 미존재 (valueOperations.get 기본 null 반환)
        // invalidateAll 진입 — scan 호출 즉시 인프라 장애로 실패
        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertThatThrownBy(() -> refreshTokenService.rotate(null, ClientType.APP, REFRESH_TOKEN))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("재사용");

        // invalidateAll을 시도는 했음 — 실패가 응답을 삼키지 않았음을 보장
        verify(redisTemplate).scan(any(ScanOptions.class));
    }

    @Test
    @DisplayName("Lua 스크립트 null 반환은 NOT_FOUND로 위장하지 않고 IllegalStateException으로 노출한다")
    @SuppressWarnings("unchecked")
    void luaNullResult_isExposedAsInfrastructureError() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.rotate(null, ClientType.APP, REFRESH_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CAS script");

        // 후속 회전·세션 폐기 동작이 일절 실행되지 않아야 함
        verify(jwtService, never()).createRefreshToken(anyLong(), anyString());
        verify(redisTemplate, never()).scan(any(ScanOptions.class));
    }
}
