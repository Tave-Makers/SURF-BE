package com.tavemakers.surf.infrastructure.report.repository;

import com.tavemakers.surf.domain.report.exception.ReportRateLimitExceededException;
import com.tavemakers.surf.domain.report.repository.ReportRateLimitRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportRedisRateLimitRepository implements ReportRateLimitRepository {

    private static final String REPORT_RATE_LIMIT_KEY = "report:rate:%d";
    private static final Duration REPORT_RATE_LIMIT_TTL = Duration.ofMinutes(5);
    private static final long REPORT_RATE_LIMIT_COUNT = 3L;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void validate(Long memberId) {
        String key = REPORT_RATE_LIMIT_KEY.formatted(memberId);

        try {
            String rawCount = redisTemplate.opsForValue().get(key);
            long count = rawCount == null ? 0L : Long.parseLong(rawCount);
            if (count >= REPORT_RATE_LIMIT_COUNT) {
                throw new ReportRateLimitExceededException();
            }
        } catch (ReportRateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("신고 rate limit 확인 중 Redis 오류가 발생해 제한 없이 진행합니다. memberId={}, error={}", memberId, e.getMessage());
        }
    }

    @Override
    public void count(Long memberId) {
        String key = REPORT_RATE_LIMIT_KEY.formatted(memberId);

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, REPORT_RATE_LIMIT_TTL);
            }
        } catch (Exception e) {
            log.warn("신고 rate limit 카운트 저장 중 Redis 오류가 발생했습니다. memberId={}, error={}", memberId, e.getMessage());
        }
    }
}
