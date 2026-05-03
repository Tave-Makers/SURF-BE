package com.tavemakers.surf.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

@EnableAsync
@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("logForwardExecutor")
    public Executor logForwardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        AtomicLong rejectedCount = new AtomicLong();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("log-fwd-");
        // 분석 서버 전송 실패는 서비스 처리와 분리하되, 누락 여부는 추적 가능해야 한다.
        executor.setRejectedExecutionHandler((r, e) -> logRejectedTask(e, rejectedCount.incrementAndGet()));
        executor.initialize();
        return executor;
    }

    private void logRejectedTask(ThreadPoolExecutor executor, long rejectedCount) {
        if (rejectedCount == 1 || rejectedCount % 100 == 0) {
            log.warn("[LogForward] 큐 초과로 전송 태스크 폐기 (누적={}, queueSize={}, remainingCapacity={})",
                    rejectedCount,
                    executor.getQueue().size(),
                    executor.getQueue().remainingCapacity());
        }
    }
}
