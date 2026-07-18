package com.tavemakers.surf.domain.post.service.support;

import com.tavemakers.surf.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private static final String VIEW_COUNT_KEY = "post:%d:view:count";
    private static final String VIEWERS_KEY = "post:%d:viewers:%d";
    private static final Duration VIEWERS_TTL = Duration.ofDays(1);
    // 스케줄러(1시간 주기)가 회수하지 못한 델타 키가 무기한 남지 않도록 하는 안전망 TTL
    private static final Duration VIEW_COUNT_TTL = Duration.ofDays(2);

    // 첫 조회 판정 + 델타 증가 + TTL + 델타 조회를 한 번의 왕복으로 처리 (기존 2~5회 왕복 → 1회).
    // Redis에는 DB 미반영 증가분(델타)만 저장한다. INCR는 키가 없으면 0에서 시작한다.
    // KEYS[1]=델타 키, KEYS[2]=viewer 키, ARGV[1]=델타 TTL(초), ARGV[2]=viewer TTL(초)
    private static final DefaultRedisScript<Long> INCREASE_AND_GET_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[2]) == 0 then " +
                    "redis.call('INCR', KEYS[1]); " +
                    "redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
                    "redis.call('SET', KEYS[2], '1', 'EX', ARGV[2]) " +
                    "end; " +
                    "local delta = redis.call('GET', KEYS[1]); " +
                    "if delta then return tonumber(delta) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    /**
     * 게시글 조회수 증가 및 반환 — Redis 왕복 1회.
     * 장애 시 DB 직접 증가로 폴백하며, 이 경우 더티체킹 반영을 위해
     * 호출자(usecase/query)의 트랜잭션 안에서 호출되어야 한다.
     */
    public int increaseViewCount(Post post, Long viewerId) {
        String viewCountKey = generateViewCountKey(post.getId());
        String viewersKey = generateViewersKey(post.getId(), viewerId);

        try {
            Long delta = redisTemplate.execute(
                    INCREASE_AND_GET_SCRIPT,
                    List.of(viewCountKey, viewersKey),
                    String.valueOf(VIEW_COUNT_TTL.toSeconds()),
                    String.valueOf(VIEWERS_TTL.toSeconds())
            );
            if (delta == null) {
                throw new IllegalStateException("view count script returned null");
            }
            return post.getViewCount() + delta.intValue();
        } catch (Exception e) {
            log.error("Redis 커넥션 에러로 Database에서 조회합니다. Error: {}", e.getMessage());
            post.increaseViewCount();
            return post.getViewCount();
        }
    }

    private String generateViewCountKey(Long postId) {
        return String.format(VIEW_COUNT_KEY, postId);
    }

    private String generateViewersKey(Long postId, Long viewerId) {
        return String.format(VIEWERS_KEY, postId, viewerId);
    }

}
