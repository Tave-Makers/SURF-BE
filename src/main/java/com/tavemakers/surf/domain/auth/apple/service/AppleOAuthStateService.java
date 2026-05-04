package com.tavemakers.surf.domain.auth.apple.service;

import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/** Apple OAuth state → nonce 임시 저장소 (Redis). form_post cross-site 쿠키 차단 대응 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleOAuthStateService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "apple:oauth:state:";
    private static final long TTL_SECONDS = 300;

    /** state 키로 nonce를 Redis에 저장 */
    public void save(String state, String nonce) {
        redisTemplate.opsForValue().set(key(state), nonce, TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("[APPLE][STATE] saved state={}", state);
    }

    /** state로 nonce를 조회하고 즉시 삭제 (1회용) */
    public String popNonce(String state) {
        String redisKey = key(state);
        String nonce = redisTemplate.opsForValue().get(redisKey);
        if (nonce == null) {
            log.warn("[APPLE][STATE] state not found or expired state={}", state);
            throw new AppleAuthException(
                    AppleAuthErrorMessage.INVALID_STATE.getStatus(),
                    AppleAuthErrorMessage.INVALID_STATE.getMessage()
            );
        }
        redisTemplate.delete(redisKey);
        log.debug("[APPLE][STATE] popped state={}", state);
        return nonce;
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
