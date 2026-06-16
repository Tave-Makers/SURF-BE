package com.tavemakers.surf.domain.auth.common.service;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService.RotateResult;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static JwtService jwtService;
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

        jwtService = new JwtService(env);
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
    @DisplayName("동시 rotate 2건 — 재사용 탐지 없이 같은 새 토큰으로 수렴한다")
    void concurrentRotate_convergesWithoutReuseDetection() throws InterruptedException {
        Long memberId = 1L;
        String refreshToken = refreshTokenService.issueRaw(memberId, "device-conc");
        refreshTokenService.issueRaw(memberId, "device-other");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<String> successTokens = new CopyOnWriteArrayList<>();
        AtomicInteger reuseCount = new AtomicInteger();
        AtomicInteger notFoundCount = new AtomicInteger();

        Runnable task = () -> {
            ready.countDown();
            try {
                start.await();
                RotateResult result = refreshTokenService.rotate(null, ClientType.APP, refreshToken);
                successTokens.add(result.newRefreshToken());
            } catch (UnauthorizedException e) {
                if (e.getMessage() != null && e.getMessage().contains("재사용")) {
                    reuseCount.incrementAndGet();
                } else {
                    notFoundCount.incrementAndGet();
                }
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

        // 패자가 전 기기 로그아웃(invalidateAll)을 유발해선 안 됨
        assertThat(reuseCount.get()).isZero();
        // CAS 승자는 항상 성공하며, 모든 성공은 동일한 새 토큰을 받아야 함 (멱등)
        assertThat(successTokens).isNotEmpty();
        assertThat(successTokens.stream().distinct().count()).isEqualTo(1);
        // 정확히 한 개의 새 토큰만 현재 키로 살아남음
        assertThat(redisTemplate.opsForValue().get("refresh:" + memberId + ":device-conc"))
                .isEqualTo(successTokens.get(0));
        // 다른 디바이스 세션은 영향받지 않아야 함
        assertThat(redisTemplate.hasKey("refresh:" + memberId + ":device-other")).isTrue();
    }

    @Test
    @DisplayName("이미 회전된 토큰으로 재요청하면 직전 발급된 새 토큰을 멱등 반환한다")
    void retryWithConsumedToken_returnsSameNewTokenIdempotently() {
        Long memberId = 2L;
        String oldToken = refreshTokenService.issueRaw(memberId, "device-A");
        refreshTokenService.issueRaw(memberId, "device-other");

        RotateResult first = refreshTokenService.rotate(null, ClientType.APP, oldToken);
        RotateResult retry = refreshTokenService.rotate(null, ClientType.APP, oldToken);

        // 재요청은 새 토큰을 다시 발급하지 않고 직전 결과를 그대로 반환
        assertThat(retry.newRefreshToken()).isEqualTo(first.newRefreshToken());
        // 멱등 처리이므로 재사용 탐지(invalidateAll)가 트리거되지 않음
        assertThat(redisTemplate.hasKey("refresh:" + memberId + ":device-other")).isTrue();
    }

    @Test
    @DisplayName("회전 이력이 없는 미지의 유효 토큰은 재사용으로 탐지돼 전 세션을 폐기한다")
    void rotateWithUnknownToken_triggersReuseDetection() {
        Long memberId = 3L;
        refreshTokenService.issueRaw(memberId, "device-A");
        refreshTokenService.issueRaw(memberId, "device-other");

        // 서명은 유효하지만 저장·회전된 적 없는 토큰 → 유예 키가 존재하지 않음
        String unknownToken = jwtService.createRefreshToken(memberId, "device-A");

        assertThatThrownBy(() -> refreshTokenService.rotate(null, ClientType.APP, unknownToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("재사용");

        // 재사용 탐지 시 해당 회원의 모든 디바이스 세션이 폐기됨
        assertThat(redisTemplate.hasKey("refresh:" + memberId + ":device-A")).isFalse();
        assertThat(redisTemplate.hasKey("refresh:" + memberId + ":device-other")).isFalse();
    }

    @Test
    @DisplayName("로그아웃(invalidate)은 유예 키까지 폐기해 멱등 회전을 차단한다")
    void invalidate_clearsGraceKey() {
        Long memberId = 4L;
        String oldToken = refreshTokenService.issueRaw(memberId, "device-A");
        refreshTokenService.rotate(null, ClientType.APP, oldToken);

        refreshTokenService.invalidate(memberId, "device-A");

        assertThat(redisTemplate.hasKey("refresh:" + memberId + ":grace:device-A")).isFalse();
        // 유예 키가 사라졌으므로 직전 토큰 재요청은 멱등 반환 없이 실패
        assertThatThrownBy(() -> refreshTokenService.rotate(null, ClientType.APP, oldToken))
                .isInstanceOf(UnauthorizedException.class);
    }
    
    @Test
    @DisplayName("SCAN 배치 크기를 초과하는 세션도 invalidateAll로 누락 없이 전부 삭제한다")
    void invalidateAll_deletesAllDeviceSessions() {
        Long memberId = 5L;
        // SCAN_SIZE(100)를 초과하는 키 수 → 커서가 한 바퀴 이상 돌고 버퍼가 중간에 flush되는
        // 경계 동작을 강제한다. 110은 현실적 디바이스 수가 아니라 배치 경계(100) 검증용 값이다.
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
