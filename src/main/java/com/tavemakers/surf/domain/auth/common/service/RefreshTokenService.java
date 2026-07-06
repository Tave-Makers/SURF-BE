package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.exception.TokenErrorMessage;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
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

    // GET + DEL 원자적 실행 — 동시 refresh 경합 시 정확히 한 요청만 회전에 성공하게 한다
    // (AppleOAuthStateService의 검증된 패턴 재사용)
    private static final DefaultRedisScript<String> GET_AND_DEL_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]) end; return v",
            String.class
    );

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

        // 조회+삭제를 원자적으로 실행: GET→비교→DELETE→SET 비원자 시퀀스에서는 동시 refresh 2건이
        // 모두 회전에 성공해 이후 회전에서 오탐 REUSE_DETECTED(전 세션 폐기)가 발생할 수 있다.
        // 원자화하면 경합 패자는 stored=null(NOT_FOUND, 해당 디바이스 재로그인)로 격하된다.
        String stored = redisTemplate.execute(GET_AND_DEL_SCRIPT, List.of(key));

        if (stored == null) {
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }

        // refresh reuse detection
        if (!refreshToken.equals(stored)) {
            log.error("[RTR][ROTATE] refresh reuse detected memberId={}", memberId);
            invalidateAll(memberId);
            throw new UnauthorizedException(TokenErrorMessage.REFRESH_TOKEN_REUSE_DETECTED.getMessage());
        }

        // ROTATION — 같은 key에 SET overwrite 하므로 별도 delete가 필요 없고,
        // delete→save 사이 장애로 세션이 유실되는 창도 없다
        log.info("[RTR][ROTATE] rotation allowed");

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

    /** refresh 재사용 탐지 시 전체 세션 폐기 */
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
