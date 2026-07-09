package com.tavemakers.surf.domain.post.service.support;

import com.tavemakers.surf.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private static final String VIEW_COUNT_KEY = "post:%d:view:count";
    private static final String VIEWERS_KEY = "post:%d:viewers:%d";
    private static final Duration VIEWERS_TTL = Duration.ofDays(1);
    // 스케줄러(1시간 주기)가 회수하지 못한 델타 키가 무기한 남지 않도록 하는 안전망 TTL
    private static final Duration VIEW_COUNT_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    /** 게시글 조회수 증가 및 반환 */
    @Transactional
    public int increaseViewCount(Post post, Long viewerId) {
        String viewCountKey = generateViewCountKey(post.getId());
        String viewersKey = generateViewersKey(post.getId(), viewerId);

        try {
            Boolean alreadyViewed = redisTemplate.hasKey(viewersKey);
            if(Boolean.FALSE.equals(alreadyViewed)) {
                // Redis에는 DB 미반영 증가분(델타)만 저장한다.
                // INCR는 키가 없으면 0에서 시작하므로 별도 초기화가 필요 없다 (비원자 초기화 경합 제거).
                redisTemplate.opsForValue().increment(viewCountKey, 1);
                redisTemplate.expire(viewCountKey, VIEW_COUNT_TTL);
                redisTemplate.opsForValue().set(viewersKey, "1", VIEWERS_TTL);
            }

            String delta = redisTemplate.opsForValue().get(viewCountKey);
            return post.getViewCount() + (delta != null ? Integer.parseInt(delta) : 0);
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
