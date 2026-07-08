package com.tavemakers.surf.domain.notification.application.usecase;

import com.tavemakers.surf.domain.notification.domain.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.domain.entity.Platform;
import com.tavemakers.surf.domain.notification.domain.repository.DeviceTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FCM 무효 토큰 비활성화 영속화 회귀 테스트.
 *
 * DeviceTokenUsecase.disableTokens 는 @Transactional + 벌크 UPDATE(disableAllByTokenIn)로 실행된다.
 * 트랜잭션 밖에서 호출한 뒤 DB 를 재조회했을 때 enabled=false 가 실제로 커밋/영속화됐는지 검증한다.
 *
 * 트랜잭션 경계를 정확히 재현하기 위해 클래스 트랜잭션을 NOT_SUPPORTED 로 비활성화한다.
 * (동시성 아님 — 단순 영속화 검증)
 */
@DataJpaTest
@Import(DeviceTokenUsecase.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeviceTokenDisableTest {

    @Autowired
    private DeviceTokenUsecase deviceTokenUsecase;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            DeviceToken a = DeviceToken.builder()
                    .memberId(1L).token("token-a").platform(Platform.ANDROID).build();
            DeviceToken b = DeviceToken.builder()
                    .memberId(2L).token("token-b").platform(Platform.IOS).build();
            entityManager.persist(a);
            entityManager.persist(b);

            this.tokenA = a.getToken();
            this.tokenB = b.getToken();
        });
    }

    @Test
    @DisplayName("disableTokens 호출 후 DB 재조회 시 해당 토큰들의 enabled 가 false 로 영속화된다")
    void 무효토큰_비활성화_영속화() {
        // 사전 조건: 두 토큰 모두 enabled=true
        assertThat(loadEnabled(tokenA)).isTrue();
        assertThat(loadEnabled(tokenB)).isTrue();

        deviceTokenUsecase.disableTokens(List.of(tokenA, tokenB));

        assertThat(loadEnabled(tokenA))
                .as("token-a 가 비활성화되어 영속화되어야 한다")
                .isFalse();
        assertThat(loadEnabled(tokenB))
                .as("token-b 가 비활성화되어 영속화되어야 한다")
                .isFalse();
    }

    /** 새 읽기 트랜잭션에서 토큰의 enabled 상태를 재조회한다. */
    private boolean loadEnabled(String token) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return Boolean.TRUE.equals(tx.execute(status ->
                deviceTokenRepository.findByToken(token).orElseThrow().isEnabled()));
    }
}
