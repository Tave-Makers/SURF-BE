package com.tavemakers.surf.domain.auth.apple.service;

import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Apple OAuth state → nonce 임시 저장소 (Redis). form_post cross-site 쿠키 차단 대응 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleOAuthStateService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "apple:oauth:state:";
    private static final long TTL_SECONDS = 300;

    // GET + DEL 원자적 실행 — 동시 요청이 같은 nonce를 두 번 사용하는 replay 공격 방지
    private static final DefaultRedisScript<String> GET_AND_DEL_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]) end; return v",
            String.class
    );

    /** state 키로 nonce를 Redis에 저장 */
    public void save(String state, String nonce) {
        redisTemplate.opsForValue().set(key(state), nonce, TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("[APPLE][STATE] saved state={}", state);
    }

    /** state로 nonce를 원자적으로 조회·삭제 (1회용). 없거나 만료 시 INVALID_STATE 예외 */
    public String popNonce(String state) {
        String nonce = redisTemplate.execute(GET_AND_DEL_SCRIPT, List.of(key(state)));
        if (nonce == null) {
            log.warn("[APPLE][STATE] state not found or expired state={}", state);
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_STATE.getStatus(),
                    AppleAuthErrorMessage.INVALID_STATE.getMessage()
            );
        }
        log.debug("[APPLE][STATE] popped state={}", state);
        return nonce;
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
