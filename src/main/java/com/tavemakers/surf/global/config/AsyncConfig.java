package com.tavemakers.surf.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("logForwardExecutor")
    public Executor logForwardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("log-fwd-");
        // 큐 초과 시 조용히 버림 — 로컬 파일에 이미 남아있으므로 서비스에 영향 없음
        executor.setRejectedExecutionHandler((r, e) -> {});
        executor.initialize();
        return executor;
    }
}
