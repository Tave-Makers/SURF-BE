package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.exception.MemberBadgeAlreadyExistsException;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.support.ConcurrencyTestHelper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * 배지 동시 부여 race 회귀 테스트 (unique 제약 + 도메인 예외 변환 검증).
 *
 * 같은 (badgeId, memberIds)로 두 스레드가 동시에 assignBadge 를 호출하면,
 * 사전 존재 체크를 둘 다 통과하더라도 member_badge 의 (member_id, badge_id) unique 제약이 하나를 막는다.
 * MemberBadgeAssignService 는 saveAllAndFlush 로 즉시 flush 해 DataIntegrityViolationException 을
 * 잡아 MemberBadgeAlreadyExistsException 으로 변환한다.
 *
 * 검증: 정확히 1회 성공, 실패 1회는 MemberBadgeAlreadyExistsException,
 *       MemberBadge row 는 회원 수만큼만 존재. DataIntegrityViolationException 이 밖으로 새면 실패.
 *
 * 각 스레드가 독립 트랜잭션으로 커밋해야 race 가 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED 로 비활성화한다.
 */
@DataJpaTest
@Import(MemberBadgeAssignService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class MemberBadgeAssignConcurrencyTest {

    private static final int CONCURRENT_ASSIGNS = 2;

    @Autowired
    private MemberBadgeAssignService memberBadgeAssignService;

    @Autowired
    private MemberBadgeRepository memberBadgeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 배지 부여 대상 회원 조회는 mock. 실제 저장한 Member 리스트를 반환한다.
    @MockBean
    private MemberGetService memberGetService;

    private Long badgeId;
    private List<Long> memberIds;

    @BeforeEach
    void setUp() {
        // 클래스 트랜잭션(NOT_SUPPORTED)이 없으므로, 준비 데이터는 별도 트랜잭션으로 명시적 커밋한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Badge badge = new Badge("우수회원", "url", "설명", "요건");
            entityManager.persist(badge);
            this.badgeId = badge.getId();

            memberIds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Member m = persistMember("badge" + i + "@test.com");
                memberIds.add(m.getId());
            }
        });

        // getMembersByIds 는 매 호출 시 새 읽기 트랜잭션에서 회원을 로드해 반환한다.
        given(memberGetService.getMembersByIds(anyList()))
                .willAnswer(inv -> loadMembersInReadTx(inv.getArgument(0)));
    }

    private List<Member> loadMembersInReadTx(List<Long> ids) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            List<Member> members = new ArrayList<>();
            for (Long id : ids) {
                members.add(entityManager.find(Member.class, id));
            }
            return members;
        });
    }

    private Member persistMember(String email) {
        Member member = Member.builder()
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
    @DisplayName("같은 배지를 동일 회원들에게 동시에 부여하면 1건만 성공하고 나머지는 MemberBadgeAlreadyExistsException 이며 row 는 회원 수만큼만 존재한다")
    void 동시_배지부여_race_에서_중복은_도메인예외로_변환되고_row는_회원수만큼만() throws InterruptedException {
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_ASSIGNS,
                () -> memberBadgeAssignService.assignBadge(badgeId, memberIds)
        );

        long rowCount = loadRowCountInReadTx();

        assertThat(result.successCount())
                .as("동시 부여 2건 중 정확히 1건만 성공해야 한다")
                .isEqualTo(1);
        assertThat(result.failureCount())
                .as("나머지 1건은 실패해야 한다")
                .isEqualTo(1);
        assertThat(result.failures())
                .as("실패는 MemberBadgeAlreadyExistsException 이어야 한다 (DataIntegrityViolationException 이 새면 안 됨)")
                .allSatisfy(t -> {
                    assertThat(t).isInstanceOf(MemberBadgeAlreadyExistsException.class);
                    assertThat(t).isNotInstanceOf(DataIntegrityViolationException.class);
                });
        assertThat(rowCount)
                .as("MemberBadge row 는 회원 수(%d)만큼만 존재해야 한다", memberIds.size())
                .isEqualTo(memberIds.size());
    }

    private long loadRowCountInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> memberBadgeRepository.count());
    }
}
