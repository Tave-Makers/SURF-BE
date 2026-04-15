package com.tavemakers.surf.domain.auth.service;

import com.tavemakers.surf.domain.auth.exception.AuthErrorMessage;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    /** Redis key: refresh:{memberId}:{deviceId} */
    private static final String KEY_PREFIX = "refresh:";

    /** 로그인 시 refresh 발급 + 저장 + 쿠키 반환 */
    public ResponseCookie issue(Long memberId, String deviceId) {
        String refreshToken = jwtService.createRefreshToken(memberId, deviceId);
        save(refreshToken);
        log.info("[RTR][ISSUE] refresh token cookie built");
        return jwtService.buildRefreshTokenCookie(refreshToken);
    }

    /** RTR 핵심: refresh 검증 + 재사용 탐지 + 회전(rotation) */
    public Long rotate(HttpServletResponse response, String refreshToken) {
        boolean valid = jwtService.isTokenValid(refreshToken);
        log.info("[RTR][ROTATE] isTokenValid={}", valid);

        if (!valid) {
            throw new UnauthorizedException(AuthErrorMessage.REFRESH_TOKEN_INVALID.getMessage());
        }

        Long memberId = jwtService.extractMemberId(refreshToken).orElseThrow();
        String deviceId = jwtService.extractDeviceId(refreshToken).orElseThrow();

        log.info("[RTR][ROTATE] extracted memberId={}", memberId);

        String key = key(memberId, deviceId);
        log.debug("[RTR][ROTATE] redisKey generated");

        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new UnauthorizedException(AuthErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        // refresh reuse detection
        if (!refreshToken.equals(stored)) {
            log.error("[RTR][ROTATE] refresh reuse detected memberId={}", memberId);
            invalidateAll(memberId);
            throw new UnauthorizedException(AuthErrorMessage.REFRESH_TOKEN_REUSE_DETECTED.getMessage());
        }

        // ROTATION
        log.info("[RTR][ROTATE] rotation allowed, deleting old token");
        redisTemplate.delete(key);

        String newRefresh = jwtService.createRefreshToken(memberId, deviceId);
        save(newRefresh);
        jwtService.sendRefreshToken(response, newRefresh);

        log.info("[RTR][ROTATE] rotation success memberId={}", memberId);
        return memberId;
    }

    /** 특정 디바이스 refresh 무효화 (로그아웃) */
    public void invalidate(Long memberId, String deviceId) {
        redisTemplate.delete(key(memberId, deviceId));
    }

    /**
     * refresh 재사용 탐지 시 전체 세션 폐기
     */
     // TODO: redisTemplate.keys()는 O(N) 블로킹 명령으로 프로덕션 Redis에서 서비스 장애를 유발할 수 있음.
     // TODO: SCAN 커서 기반 명령(redisTemplate.scan())으로 교체 필요.

    public void invalidateAll(Long memberId) {
        log.warn("[RTR][INVALIDATE-ALL] start memberId={}", memberId);

        String pattern = KEY_PREFIX + memberId + ":*";
        log.info("[RTR][INVALIDATE-ALL] pattern={}", pattern);

        Set<String> keys = redisTemplate.keys(pattern);
        log.info("[RTR][INVALIDATE-ALL] foundKeyCount={}",
                keys == null ? 0 : keys.size());

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("[RTR][INVALIDATE-ALL] deleted keyCount={}", keys.size());
        } else {
            log.info("[RTR][INVALIDATE-ALL] no keys to delete");
        }
    }

    /* ================= 내부 유틸 ================= */

    private void save(String refreshToken) {
        Long memberId = jwtService.extractMemberId(refreshToken).orElseThrow();
        String deviceId = jwtService.extractDeviceId(refreshToken).orElseThrow();

        log.info("[RTR][INVALIDATE] start memberId={} deviceId={}", memberId, deviceId);

        long ttlMs = jwtService.getExpiration(refreshToken) - System.currentTimeMillis();
        if (ttlMs <= 0) {
            throw new IllegalStateException("Refresh token already expired");
        }
        String redisKey = key(memberId, deviceId);

        redisTemplate.opsForValue()
                .set(redisKey, refreshToken, ttlMs, TimeUnit.MILLISECONDS);
        log.info("[RTR] refresh token saved. key={}, ttlMs={}", redisKey, ttlMs);
    }

    private String key(Long memberId, String deviceId) {
        return KEY_PREFIX + memberId + ":" + deviceId;
    }
}
