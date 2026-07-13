package com.tavemakers.surf.application.post.scheduler;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import com.tavemakers.surf.domain.post.mapper.PostMapper;
import com.tavemakers.surf.infrastructure.post.repository.PostJdbcRepository;
import com.tavemakers.surf.global.common.aop.annotations.ExecutionTimeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ViewCountScheduler {

    private final StringRedisTemplate redisTemplate;
    private final PostMapper postMapper;
    private final PostJdbcRepository postJdbcRepository;

    private static final String VIEW_COUNT_PATTERN = "post:*:view:count";
    private static final int SCAN_SIZE = 100;

    // GET + DEL 원자적 실행 — 델타 회수와 삭제 사이에 끼어드는 INCR 유실을 방지한다
    // (auth 도메인 RefreshTokenService의 검증된 패턴을 도메인 경계 준수를 위해 자체 상수로 복제)
    private static final DefaultRedisScript<String> GET_AND_DEL_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]) end; return v",
            String.class
    );

    @Scheduled(cron = "0 0 * * * *")
    @ExecutionTimeLog(jobName = "조회수 동기화 작업")
    public void synchronizeViewCount() {
        ScanOptions scanOption = ScanOptions.scanOptions()
                .match(VIEW_COUNT_PATTERN)
                .count(SCAN_SIZE)
                .build();

        List<String> keyBuffer = new ArrayList<>(SCAN_SIZE);
        try (Cursor<String> cursor = redisTemplate.scan(scanOption)) {
            while (cursor.hasNext()) {
                keyBuffer.add(cursor.next());

                if (keyBuffer.size() >= SCAN_SIZE) {
                    processBulkUpdate(keyBuffer);
                    keyBuffer.clear();
                }
            }

            if (!keyBuffer.isEmpty()) {
                processBulkUpdate(keyBuffer);
            }
        } catch (Exception e) {
            log.error("조회수 동기화 작업에 실패했습니다.", e);
        }
    }

    private void processBulkUpdate(List<String> viewCountKeys) {
        List<PostViewUpdateDto> updateDtoList = collectViewCountDeltas(viewCountKeys);
        executeViewCountUpdate(updateDtoList);
    }

    /**
     * 델타를 GET+DEL로 원자 회수한다. 회수 이후 DB 반영이 실패하면 해당 델타는 유실될 수 있으나,
     * 기존에도 동기화 실패는 로그만 남기는 수준이므로 재적재 로직은 두지 않는다.
     */
    private List<PostViewUpdateDto> collectViewCountDeltas(List<String> keys) {
        return keys.stream()
                .map(key -> {
                    String value = redisTemplate.execute(GET_AND_DEL_SCRIPT, List.of(key));
                    return value != null ? postMapper.toUpdateDto(key, value) : null;
                })
                .filter(Objects::nonNull)
                .filter(dto -> dto.viewCountDelta() != 0)
                .toList();
    }

    private void executeViewCountUpdate(List<PostViewUpdateDto> updateDtoList) {
        if (!updateDtoList.isEmpty()) {
            postJdbcRepository.viewCountBulkUpdate(updateDtoList);
        }
    }

}
