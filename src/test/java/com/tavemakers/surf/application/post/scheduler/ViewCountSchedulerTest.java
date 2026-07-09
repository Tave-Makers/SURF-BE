package com.tavemakers.surf.application.post.scheduler;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import com.tavemakers.surf.domain.post.mapper.PostMapper;
import com.tavemakers.surf.domain.post.service.support.PostUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 조회수 동기화 스케줄러 델타 회수 단위 테스트.
 *
 * 과거에는 multiGet(비파괴 읽기)으로 키가 무기한 남고 절대값을 DB에 덮어썼다.
 * 현재는 GET+DEL로 델타를 원자 회수하며, 델타 0·null·파싱 불가는 skip한다.
 */
@ExtendWith(MockitoExtension.class)
class ViewCountSchedulerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PostUpdateService postUpdateService;

    @Test
    @DisplayName("GET+DEL로 회수한 델타 중 0·null·파싱 불가는 제외하고 DB에 반영한다")
    void 유효한_델타만_DB에_반영한다() {
        ViewCountScheduler scheduler = new ViewCountScheduler(redisTemplate, new PostMapper(), postUpdateService);
        stubScan(List.of("post:1:view:count", "post:2:view:count", "post:3:view:count", "post:4:view:count"));
        stubGetAndDel("post:1:view:count", "3");   // 유효 델타
        stubGetAndDel("post:2:view:count", "0");   // 델타 0 → skip
        stubGetAndDel("post:3:view:count", null);  // 키 없음 → skip
        stubGetAndDel("post:4:view:count", "abc"); // 파싱 불가 → skip

        scheduler.synchronizeViewCount();

        ArgumentCaptor<List<PostViewUpdateDto>> captor = ArgumentCaptor.captor();
        verify(postUpdateService).updateViewCount(captor.capture());
        assertThat(captor.getValue()).containsExactly(new PostViewUpdateDto(1L, 3));
    }

    @Test
    @DisplayName("회수한 유효 델타가 없으면 DB 업데이트를 호출하지 않는다")
    void 유효_델타가_없으면_DB_업데이트를_생략한다() {
        ViewCountScheduler scheduler = new ViewCountScheduler(redisTemplate, new PostMapper(), postUpdateService);
        stubScan(List.of("post:1:view:count"));
        stubGetAndDel("post:1:view:count", "0");

        scheduler.synchronizeViewCount();

        verify(postUpdateService, never()).updateViewCount(any());
    }

    @SuppressWarnings("unchecked")
    private void stubScan(List<String> keys) {
        Cursor<String> cursor = mock(Cursor.class);
        Iterator<String> iterator = keys.iterator();
        when(cursor.hasNext()).thenAnswer(inv -> iterator.hasNext());
        when(cursor.next()).thenAnswer(inv -> iterator.next());
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    }

    private void stubGetAndDel(String key, String value) {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)))).thenReturn(value);
    }
}
