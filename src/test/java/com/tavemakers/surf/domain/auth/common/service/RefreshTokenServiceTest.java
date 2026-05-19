package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.global.common.exception.UnauthorizedException;
import com.tavemakers.surf.global.jwt.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, String> redisTemplate;
    private static RefreshTokenService refreshTokenService;

    @BeforeAll
    static void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379)
        );
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        StringRedisSerializer serializer = new StringRedisSerializer();
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});

        JwtService jwtService = new JwtService(env);
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "testsecrettestsecrettestsecrettestsecrettestsecret");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpireMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpireMs", 604800000L);
        jwtService.init();

        refreshTokenService = new RefreshTokenService(jwtService, redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.stop();
        connectionFactory.destroy();
    }

    @BeforeEach
    void flushRedis() {
        redisTemplate.execute((RedisCallback<Void>) conn -> {
            conn.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("동시 rotate 요청 2개 중 정확히 1개만 성공한다")
    void concurrentRotate_onlyOneThreadSucceeds() throws InterruptedException {
        String refreshToken = refreshTokenService.issueRaw(1L, "device-conc");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        Runnable task = () -> {
            ready.countDown();
            try {
                start.await();
                refreshTokenService.rotate(null, ClientType.APP, refreshToken);
                successCount.incrementAndGet();
            } catch (UnauthorizedException e) {
                failCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();

        ready.await();
        start.countDown();

        t1.join(5000);
        t2.join(5000);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("110개 디바이스 세션을 invalidateAll로 전부 삭제한다")
    void invalidateAll_deletesAllDeviceSessions() {
        Long memberId = 2L;
        int deviceCount = 110;

        for (int i = 0; i < deviceCount; i++) {
            refreshTokenService.issueRaw(memberId, "device-" + i);
        }

        assertThat(scanKeys("refresh:" + memberId + ":*")).hasSize(deviceCount);

        refreshTokenService.invalidateAll(memberId);

        assertThat(scanKeys("refresh:" + memberId + ":*")).isEmpty();
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        try (var cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build())) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }
}
