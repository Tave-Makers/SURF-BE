package com.tavemakers.surf.domain.post.service.support;

import com.tavemakers.surf.domain.post.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 조회수 델타 방식 단위 테스트.
 *
 * 과거에는 hasKey → set(DB 절대값) → INCR 비원자 시퀀스로 동시 첫 조회 시 증가가 유실되고,
 * Redis 절대값이 DB와 어긋날 수 있었다. 현재는 Redis에 델타만 INCR(키 없으면 0에서 시작)하고
 * 반환값은 DB 기준값 + 델타로 계산한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ViewCountServiceTest {

    private static final Long POST_ID = 1L;
    private static final Long VIEWER_ID = 100L;
    private static final String VIEW_COUNT_KEY = "post:1:view:count";
    private static final String VIEWERS_KEY = "post:1:viewers:100";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ViewCountService viewCountService;

    private Post post;

    @BeforeEach
    void setUp() {
        viewCountService = new ViewCountService(redisTemplate);
        post = Post.builder().id(POST_ID).title("제목").content("내용").viewCount(10).build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("첫 조회 시 절대값 초기화 없이 바로 INCR하고, DB값 + 델타를 반환한다")
    void 첫_조회는_초기화_없이_INCR하고_DB값과_델타를_합산해_반환한다() {
        when(redisTemplate.hasKey(VIEWERS_KEY)).thenReturn(false);
        when(valueOperations.get(VIEW_COUNT_KEY)).thenReturn("3");

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).as("DB 기준값(10) + Redis 델타(3)").isEqualTo(13);
        verify(valueOperations).increment(VIEW_COUNT_KEY, 1);
        // 절대값 초기화 set이 사라졌는지 검증 (비원자 초기화 경합 제거)
        verify(valueOperations, never()).set(eq(VIEW_COUNT_KEY), anyString());
        // 안전망 TTL 부여 검증 (키 무기한 누적 방지)
        verify(redisTemplate).expire(eq(VIEW_COUNT_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("이미 조회한 사용자는 증가 없이 DB값 + 델타를 반환한다")
    void 중복_조회는_증가_없이_합산값만_반환한다() {
        when(redisTemplate.hasKey(VIEWERS_KEY)).thenReturn(true);
        when(valueOperations.get(VIEW_COUNT_KEY)).thenReturn("2");

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).isEqualTo(12);
        verify(valueOperations, never()).increment(anyString(), eq(1L));
    }

    @Test
    @DisplayName("델타 키가 없으면(동기화 직후) DB값 + 0을 반환한다")
    void 델타_키가_없으면_DB값을_반환한다() {
        when(redisTemplate.hasKey(VIEWERS_KEY)).thenReturn(true);
        when(valueOperations.get(VIEW_COUNT_KEY)).thenReturn(null);

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).isEqualTo(10);
    }

    @Test
    @DisplayName("Redis 장애 시 DB 직접 증가로 폴백하고 DB값을 반환한다")
    void Redis_장애_시_DB_증가로_폴백한다() {
        when(redisTemplate.hasKey(VIEWERS_KEY)).thenThrow(new RuntimeException("connection refused"));

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result)
                .as("폴백으로 DB 조회수가 직접 증가되어야 한다 (델타 방식이므로 복구 후 덮어쓰기 유실 없음)")
                .isEqualTo(11);
        assertThat(post.getViewCount()).isEqualTo(11);
    }
}
