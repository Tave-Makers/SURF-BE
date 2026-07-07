package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.support.ConcurrencyTestHelper;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 개인 활동 점수 동시 갱신 회귀 테스트 (비관적 락 검증).
 *
 * PersonalActivityScoreRepository.findByMemberIdForUpdate 가 PESSIMISTIC_WRITE(select ... for update)로
 * 행을 잠그므로, 동시 트랜잭션 N개가 같은 회원 점수를 조회 → updateScore → 커밋해도 lost update 없이
 * 최종 score = 초기값 + N 이어야 한다.
 *
 * 각 스레드가 독립 트랜잭션으로 커밋해야 락 경합이 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED 로 비활성화하고,
 * 스레드마다 TransactionTemplate 으로 자체 트랜잭션을 연다.
 */
@DataJpaTest
@Import(PersonalScoreGetService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class PersonalScoreConcurrencyTest {

    private static final int CONCURRENT_UPDATES = 10;
    private static final BigDecimal INITIAL_SCORE = BigDecimal.valueOf(100);

    @Autowired
    private PersonalScoreGetService personalScoreGetService;

    @Autowired
    private PersonalActivityScoreRepository personalScoreRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long memberId;

    @BeforeEach
    void setUp() {
        // 클래스 트랜잭션(NOT_SUPPORTED)이 없으므로, 준비 데이터는 별도 트랜잭션으로 명시적 커밋한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Member member = persistMember("scorer@test.com");
            PersonalActivityScore score = PersonalActivityScore.builder()
                    .member(member)
                    .score(INITIAL_SCORE)
                    .rewardPrefixSum(BigDecimal.ZERO)
                    .penaltyPrefixSum(BigDecimal.ZERO)
                    .build();
            entityManager.persist(score);

            this.memberId = member.getId();
        });
    }

    private Member persistMember(String email) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
                .name("회원")
                .email(email)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }

    @Test
    @DisplayName("동시 트랜잭션 10개가 같은 회원 점수를 ForUpdate 로 잠그고 갱신하면 최종 점수는 초기값+10 이다")
    void 동시_점수갱신_10건이면_lost_update_없이_반영된다() throws InterruptedException {
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_UPDATES,
                this::updateScoreInOwnTx
        );

        BigDecimal finalScore = loadScoreInReadTx();

        assertThat(result.failureCount()).isZero();
        assertThat(finalScore)
                .as("비관적 락 덕분에 동시 갱신 %d건이 lost update 없이 모두 반영되어야 한다", CONCURRENT_UPDATES)
                .isEqualByComparingTo(INITIAL_SCORE.add(BigDecimal.valueOf(CONCURRENT_UPDATES)));
    }

    /** 스레드별 독립 트랜잭션: ForUpdate 조회 → updateScore(1) → 커밋 */
    private void updateScoreInOwnTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            PersonalActivityScore score = personalScoreGetService.getPersonalScoreForUpdate(memberId);
            score.updateScore(BigDecimal.ONE);
        });
    }

    private BigDecimal loadScoreInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status ->
                personalScoreRepository.findByMemberId(memberId).orElseThrow().getScore());
    }
}
