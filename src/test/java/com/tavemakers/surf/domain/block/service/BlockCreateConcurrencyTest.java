package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.exception.BlockAlreadyExistsException;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlockCreateService.create 동시성 회귀 테스트.
 *
 * <p>순차 중복은 exists 사전 검사에서 끝나므로, saveAndFlush의 unique 위반을 잡아
 * 409로 매핑하는 catch 경로는 <b>동시 요청에서만</b> 실행된다. 그 경로를 실제로 태운다.
 *
 * <p>기대 동작: 승자 1건만 커밋되고 패자는 전부 {@link BlockAlreadyExistsException}.
 * {@code DataIntegrityViolationException}이나 {@code UnexpectedRollbackException}이 새어나오면 회귀다.
 *
 * <p>각 스레드가 독립 트랜잭션으로 커밋해야 race가 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED로 비활성화하고,
 * 스레드별로 {@link TransactionTemplate}을 열어 그 안에서 create를 호출한다. BlockCreateService에는
 * @Transactional이 없으므로(R4 — 경계는 usecase 소유) 감싸지 않으면 exists 조회와 saveAndFlush가 서로 다른
 * 트랜잭션에서 실행되어, PR2의 {@code BlockUsecase.@Transactional} 안에서 돌 운영 경계와 달라진다.
 * 감싸면 "하나의 트랜잭션에서 exists → saveAndFlush → 커밋/롤백"이라는 실제 흐름을 그대로 재현한다.
 * 이 경계에서는 승자가 커밋할 때까지 패자의 exists가 행을 보지 못하므로, 패자 전원이 catch 경로를
 * 확실히 타게 된다(로컬 4회 반복 실행에서 매번 패자 4명 전부 catch 경로).
 */
@DataJpaTest
@Import(BlockCreateService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class BlockCreateConcurrencyTest {

    private static final int CONCURRENT_THREADS = 5;

    @Autowired
    private BlockCreateService blockCreateService;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long blockerId;
    private Long blockedId;

    @BeforeEach
    void setUp() {
        // 클래스 트랜잭션이 없으므로 준비 데이터는 별도 트랜잭션으로 명시적 커밋한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            // 테스트 간 데이터가 남으므로 이메일·전화번호는 실행마다 고유하게 만든다
            blockerId = persistMember("blocker").getId();
            blockedId = persistMember("blocked").getId();
        });
    }

    @Test
    @DisplayName("5스레드가 동시에 같은 대상을 차단하면 row는 1개이고 실패는 전부 BlockAlreadyExistsException이다")
    void 동시_중복_차단시_row는_1개이고_실패는_전부_도메인예외() throws InterruptedException {
        // 운영(PR2 BlockUsecase)과 같이 exists 조회와 saveAndFlush를 한 트랜잭션 안에 둔다
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_THREADS,
                () -> tx.executeWithoutResult(status -> blockCreateService.create(blockerId, blockedId))
        );

        // 불변식 ① 패자의 예외는 전부 도메인 예외(409)여야 한다.
        //          (allSatisfy는 빈 목록도 통과 — 느린 CI에서 완전 직렬화되어 나머지가 exists로 끝나도 정상)
        assertThat(result.failures())
                .as("동시 race의 패자는 전부 BlockAlreadyExistsException이어야 한다 "
                        + "(DataIntegrityViolationException/UnexpectedRollbackException이 새면 회귀)")
                .allSatisfy(t -> assertThat(t).isInstanceOf(BlockAlreadyExistsException.class));

        // 불변식 ② 승자는 정확히 1건이다.
        assertThat(result.successCount())
                .as("동시 요청 중 정확히 1건만 성공해야 한다")
                .isEqualTo(1);

        // 불변식 ③ unique 제약으로 최종 row는 정확히 1개다.
        assertThat(countBlockRowsInReadTx())
                .as("unique 제약으로 Block row는 정확히 1개여야 한다")
                .isEqualTo(1L);
    }

    private Long countBlockRowsInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(b) from Block b where b.blockerId = :blockerId and b.blockedId = :blockedId",
                        Long.class)
                .setParameter("blockerId", blockerId)
                .setParameter("blockedId", blockedId)
                .getSingleResult());
    }

    private Member persistMember(String prefix) {
        long seed = System.nanoTime();
        Member member = Member.builder()
                .name("회원")
                .email(prefix + seed + "@test.com")
                .phoneNumber(String.valueOf(seed))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }
}
