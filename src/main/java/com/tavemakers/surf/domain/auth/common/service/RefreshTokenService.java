package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.exception.TokenErrorMessage;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    /** Redis key: refresh:{memberId}:{deviceId} */
    private static final String KEY_PREFIX = "refresh:";

    /** invalidateAll SCAN 배치 크기 */
    private static final int SCAN_SIZE = 100;

    // 저장된 값이 ARGV[1]과 일치할 때만 DEL — RTR 단일 사용 보장 (CAS)
    // 반환: 1=CONSUMED, 0=MISMATCH, -1=NOT_FOUND
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
                    "if not v then return -1 end; " +
                    "if v == ARGV[1] then redis.call('DEL', KEYS[1]); return 1 end; " +
                    "return 0",
            Long.class
    );

    /** compareAndDelete 결과 */
    private enum ConsumeResult { CONSUMED, MISMATCH, NOT_FOUND }

    /** 로그인 시 refresh 발급 + 저장 + 쿠키 반환 (WEB 흐름) */
    public ResponseCookie issue(Long memberId, String deviceId) {
        String refreshToken = jwtService.createRefreshToken(memberId, deviceId);
        save(refreshToken);
        log.info("[RTR][ISSUE] refresh token cookie built");
        return jwtService.buildRefreshTokenCookie(refreshToken);
    }

    /** 로그인 시 refresh 발급 + 저장 + 토큰 문자열 반환 (APP 본문 전달용) */
    public String issueRaw(Long memberId, String deviceId) {
        String refreshToken = jwtService.createRefreshToken(memberId, deviceId);
        save(refreshToken);
        log.info("[RTR][ISSUE] refresh token raw issued (app body)");
        return refreshToken;
    }

    /**
     * RTR 회전 결과.
     * <ul>
     *   <li>WEB: {@code newRefreshToken=null} — 쿠키로 송출됨 (response 부착 완료)</li>
     *   <li>APP: {@code newRefreshToken=새 토큰} — 컨트롤러가 본문에 담아 응답</li>
     * </ul>
     */
    public record RotateResult(Long memberId, String newRefreshToken) {}

    /** RTR 핵심: refresh 검증 + 재사용 탐지 + 회전(rotation). ClientType 분기로 송출 채널 결정. */
    public RotateResult rotate(HttpServletResponse response, ClientType clientType, String refreshToken) {
        boolean valid = jwtService.isTokenValid(refreshToken);
        log.info("[RTR][ROTATE] isTokenValid={} clientType={}", valid, clientType);

        if (!valid) {
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_INVALID.getMessage());
        }

        Long memberId = jwtService.extractMemberId(refreshToken)
                .orElseThrow(()-> new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_INVALID.getMessage()));
        String deviceId = jwtService.extractDeviceId(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_INVALID.getMessage()));

        log.info("[RTR][ROTATE] extracted memberId={}", memberId);

        String key = key(memberId, deviceId);
        log.debug("[RTR][ROTATE] redisKey generated");

        // 검증 + 삭제 원자화 (CAS). 동시 회전 시 단 한 요청만 CONSUMED를 받음
        ConsumeResult consumeResult = compareAndDelete(key, refreshToken);

        if (consumeResult == ConsumeResult.NOT_FOUND) {
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        if (consumeResult == ConsumeResult.MISMATCH) {
            log.error("[RTR][ROTATE] refresh reuse detected memberId={}", memberId);
            invalidateAll(memberId);
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_REUSE_DETECTED.getMessage());
        }

        // ROTATION (CONSUMED)
        log.info("[RTR][ROTATE] rotation allowed, old token consumed atomically");

        String newRefresh = jwtService.createRefreshToken(memberId, deviceId);
        save(newRefresh);

        if (clientType == ClientType.APP) {
            log.info("[RTR][ROTATE] rotation success (app body) memberId={}", memberId);
            return new RotateResult(memberId, newRefresh);
        }
        jwtService.sendRefreshToken(response, newRefresh);
        log.info("[RTR][ROTATE] rotation success (web cookie) memberId={}", memberId);
        return new RotateResult(memberId, null);
    }

    /** 특정 디바이스 refresh 무효화 (로그아웃) */
    public void invalidate(Long memberId, String deviceId) {
        redisTemplate.delete(key(memberId, deviceId));
    }

    /** refresh 재사용 탐지 시 전체 세션 폐기 — SCAN 커서로 non-blocking 순회 */
    public void invalidateAll(Long memberId) {
        log.warn("[RTR][INVALIDATE-ALL] start memberId={}", memberId);

        String pattern = KEY_PREFIX + memberId + ":*";
        log.info("[RTR][INVALIDATE-ALL] pattern={}", pattern);

        ScanOptions scanOption = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_SIZE)
                .build();

        List<String> keyBuffer = new ArrayList<>(SCAN_SIZE);
        long totalDeleted = 0;

        try (Cursor<String> cursor = redisTemplate.scan(scanOption)) {
            while (cursor.hasNext()) {
                keyBuffer.add(cursor.next());

                if (keyBuffer.size() >= SCAN_SIZE) {
                    totalDeleted += deleteBatch(keyBuffer);
                    keyBuffer.clear();
                }
            }

            if (!keyBuffer.isEmpty()) {
                totalDeleted += deleteBatch(keyBuffer);
            }
        }

        if (totalDeleted > 0) {
            log.warn("[RTR][INVALIDATE-ALL] deleted keyCount={}", totalDeleted);
        } else {
            log.info("[RTR][INVALIDATE-ALL] no keys to delete");
        }
    }

    /* ================= 내부 유틸 ================= */

    /** 저장된 값이 expected와 일치하면 삭제 — Lua 스크립트로 단일 명령 보장 */
    private ConsumeResult compareAndDelete(String key, String expected) {
        Long result = redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(key), expected);
        if (result == null || result == -1L) {
            return ConsumeResult.NOT_FOUND;
        }
        return result == 1L ? ConsumeResult.CONSUMED : ConsumeResult.MISMATCH;
    }

    private long deleteBatch(List<String> keys) {
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0 : deleted;
    }

    private void save(String refreshToken) {
        Long memberId = jwtService.extractMemberId(refreshToken).orElseThrow();
        String deviceId = jwtService.extractDeviceId(refreshToken).orElseThrow();

        log.info("[RTR][SAVE] start memberId={} deviceId={}", memberId, deviceId);

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
