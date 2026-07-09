package com.tavemakers.surf.domain.post.service.search;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentSearchService {
    private final StringRedisTemplate redis;           // 자동 구성으로 주입됨
    private static final String KEY_FMT = "recent:%d"; // recent:{memberId}
    private static final int MAX_SIZE = 10;
    private static final Duration TTL = Duration.ofDays(30); // 필요시 0으로 두면 무기한

    /** 최근 검색어 저장 */
    @Transactional
    public void saveQuery(Long memberId, String raw) {
        if (raw == null) return;
        String q = normalize(raw);
        if (q.isEmpty()) return;

        String key = key(memberId);

        // 1) 중복 제거
        redis.opsForList().remove(key, 0, q);
        // 2) 맨 앞에 삽입
        redis.opsForList().leftPush(key, q);
        // 3) 10개로 트림
        redis.opsForList().trim(key, 0, MAX_SIZE - 1);
        // 4) TTL 갱신
        redis.expire(key, TTL);
    }

    /** 최근 검색어 10개 조회 */
    @Transactional(readOnly = true)
    public List<String> getRecent10(Long memberId) {
        String key = key(memberId);
        List<String> items = redis.opsForList().range(key, 0, MAX_SIZE - 1);
        return items == null ? List.of() : items;
    }

    /** 최근 검색어 전체 삭제 */
    @Transactional
    public void clearAll(Long memberId) {
        redis.delete(key(memberId));
    }

    /** 특정 검색어 삭제 */
    @Transactional
    public void deleteOne(Long memberId, String rawKeyword) {
        if (rawKeyword == null) return;

        String keyword = normalize(rawKeyword);
        if (keyword.isEmpty()) return;

        String key = key(memberId);

        redis.opsForList().remove(key, 0, keyword);
    }

    private String key(Long memberId) { return KEY_FMT.formatted(memberId); }

    private String normalize(String s) {
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() > 100 ? t.substring(0, 100) : t;
    }}
