package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.exception.TokenErrorMessage;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

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

    /**
     * Issue and persist a refresh token for the given member and device, and return it as an HTTP cookie.
     *
     * @param memberId the unique identifier of the member
     * @param deviceId the identifier of the device (used to scope the refresh token)
     * @return a ResponseCookie containing the newly issued refresh token
     */
    public ResponseCookie issue(Long memberId, String deviceId) {
        String refreshToken = jwtService.createRefreshToken(memberId, deviceId);
        save(refreshToken);
        log.info("[RTR][ISSUE] refresh token cookie built");
        return jwtService.buildRefreshTokenCookie(refreshToken);
    }

    /**
     * Performs refresh-token rotation: validates the provided refresh token, detects reuse, issues and stores a new refresh token, and returns the associated member id.
     *
     * @param response the HTTP response to which the new refresh token cookie will be written
     * @param refreshToken the refresh token presented by the client
     * @return the member id extracted from the validated refresh token
     * @throws UnauthorizedException if the refresh token is invalid, not found in storage, or reuse is detected
     */
    public Long rotate(HttpServletResponse response, String refreshToken) {
        boolean valid = jwtService.isTokenValid(refreshToken);
        log.info("[RTR][ROTATE] isTokenValid={}", valid);

        if (!valid) {
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_INVALID.getMessage());
        }

        Long memberId = jwtService.extractMemberId(refreshToken).orElseThrow();
        String deviceId = jwtService.extractDeviceId(refreshToken).orElseThrow();

        log.info("[RTR][ROTATE] extracted memberId={}", memberId);

        String key = key(memberId, deviceId);
        log.debug("[RTR][ROTATE] redisKey generated");

        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        // refresh reuse detection
        if (!refreshToken.equals(stored)) {
            log.error("[RTR][ROTATE] refresh reuse detected memberId={}", memberId);
            invalidateAll(memberId);
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_REUSE_DETECTED.getMessage());
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

    /**
     * Invalidates the refresh token for a specific device of a member by removing its Redis entry.
     *
     * @param memberId the member's identifier whose device refresh token will be removed
     * @param deviceId the device identifier used to locate the refresh token in Redis
     */
    public void invalidate(Long memberId, String deviceId) {
        redisTemplate.delete(key(memberId, deviceId));
    }

    /**
     * refresh 재사용 탐지 시 전체 세션 폐기
     */
    // TODO: redisTemplate.keys()는 O(N) 블로킹 명령으로 프로덕션 Redis에서 서비스 장애를 유발할 수 있음.
    /**
     * Invalidates all refresh tokens for the given member across all devices.
     *
     * <p>Finds Redis keys matching the pattern "refresh:{memberId}:*" and deletes them; if none are found,
     * no keys are deleted.</p>
     *
     * <p>Note: this implementation uses a pattern-based key lookup that can be blocking/O(N) on the Redis
     * keyspace and may need to be replaced with a cursor-based SCAN approach for production scale.</p>
     *
     * @param memberId the identifier of the member whose refresh tokens should be revoked
     */
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

    /**
     * Stores the given refresh token in Redis under the key for its member and device, setting the entry TTL to match the token's remaining lifetime.
     *
     * The method derives the memberId and deviceId from the token, computes the remaining time until token expiration, and saves the token value with that TTL.
     *
     * @throws IllegalStateException if the token is already expired (remaining TTL is zero or negative)
     */

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

    /**
     * Builds the Redis key for a member's device refresh token.
     *
     * @param memberId the member's id
     * @param deviceId the device identifier
     * @return the Redis key in the form "refresh:{memberId}:{deviceId}"
     */
    private String key(Long memberId, String deviceId) {
        return KEY_PREFIX + memberId + ":" + deviceId;
    }
}
