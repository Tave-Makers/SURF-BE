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
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 조회수 델타 방식 단위 테스트.
 *
 * 과거에는 hasKey → INCR → EXPIRE → SET → GET 다중 왕복(첫 조회 5회, 재조회 2회)이었다.
 * 성능 측정 결과 왕복 수가 운영 지연의 지배 요인이라, 판정·증가·TTL·조회를 Lua 스크립트
 * 하나로 묶어 항상 1왕복으로 처리한다. 반환값은 DB 기준값 + 델타.
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

    private ViewCountService viewCountService;

    private Post post;

    @BeforeEach
    void setUp() {
        viewCountService = new ViewCountService(redisTemplate);
        post = Post.builder().id(POST_ID).title("제목").content("내용").viewCount(10).build();
    }

    @SuppressWarnings("unchecked")
    private void givenScriptReturns(Long delta) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(delta);
    }

    @Test
    @DisplayName("스크립트 1회 호출로 DB값 + 델타를 반환한다 (왕복 1회)")
    @SuppressWarnings("unchecked")
    void 스크립트_한_번으로_DB값과_델타를_합산해_반환한다() {
        givenScriptReturns(3L);

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).as("DB 기준값(10) + Redis 델타(3)").isEqualTo(13);
        verify(redisTemplate, times(1)).execute(
                any(RedisScript.class),
                eq(List.of(VIEW_COUNT_KEY, VIEWERS_KEY)),
                anyString(), anyString());
    }

    @Test
    @DisplayName("델타가 0이면(동기화 직후) DB값을 그대로 반환한다")
    void 델타가_없으면_DB값을_반환한다() {
        givenScriptReturns(0L);

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).isEqualTo(10);
    }

    @Test
    @DisplayName("스크립트가 null을 반환하면(인프라 이상) DB 증가로 폴백한다")
    void 스크립트_null_반환은_DB_증가로_폴백한다() {
        givenScriptReturns(null);

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result).isEqualTo(11);
        assertThat(post.getViewCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("Redis 장애 시 DB 직접 증가로 폴백하고 DB값을 반환한다")
    @SuppressWarnings("unchecked")
    void Redis_장애_시_DB_증가로_폴백한다() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

        int result = viewCountService.increaseViewCount(post, VIEWER_ID);

        assertThat(result)
                .as("폴백으로 DB 조회수가 직접 증가되어야 한다 (델타 방식이므로 복구 후 덮어쓰기 유실 없음)")
                .isEqualTo(11);
        assertThat(post.getViewCount()).isEqualTo(11);
    }
}
