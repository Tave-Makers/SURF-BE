package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.application.block.event.BlockMemberDismissListener;
import com.tavemakers.surf.domain.badge.event.BadgeMemberDismissListener;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.comment.event.CommentMemberDismissListener;
import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.event.LetterMemberDismissListener;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.service.MemberWithdrawService;
import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.entity.Platform;
import com.tavemakers.surf.domain.notification.event.NotificationMemberDismissListener;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.event.ScoreMemberDismissListener;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.application.post.usecase.PostDeleteUsecase;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import com.tavemakers.surf.domain.team.service.TeamMemberCleanupService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 회원 제명(dismiss) 원자성(전체 롤백) 통합 테스트 (D1 — docs/refactoring-plan.md).
 *
 * <p>제명은 단일 @Transactional 안에서 MemberDismissedEvent 를 동기 발행하고 각 도메인 리스너가
 * 같은 트랜잭션에서 정리한다. 계정 삭제는 "전부 성공 또는 전부 롤백"이어야 하므로, 리스너 중 하나라도
 * 실패하면 이미 삭제된 다른 도메인 데이터와 member row 까지 전부 되돌아와야 한다.
 *
 * <p>이를 재현하려면 "리스너들이 이미 지운 뒤"에 실패해야 한다. 실패 지점을 테스트 전용 리스너로
 * 잡으면 @EventListener 상대 순서에 의존하게 되는데, @Import 배열 순서도 @Order 도(무순서 리스너의
 * 기본값 LOWEST_PRECEDENCE 와 동률이라) 이를 보장하지 못한다 — 실제로 확인해보면 테스트 리스너가
 * 정상 리스너보다 먼저 실행되어, 아무것도 지워지지 않은 상태를 롤백 검증하는 무의미한 테스트가 된다.
 *
 * <p>그래서 실패 지점을 이벤트 발행 <b>이후</b> 코드인 {@code careerRepository.findByMemberId} 로 옮겼다
 * ({@link MemberDismissUsecase#dismiss} 참고). 모든 동기 리스너와 명시적 정리가 끝난 뒤 예외가 나므로
 * 순서 보장 없이도 "가장 많이 지운 상태에서의 전체 롤백"이라는 가장 엄격한 시나리오가 된다.
 *
 * <p>dismiss 가 @Transactional 이므로 실제 롤백을 검증하려면 클래스 트랜잭션을 NOT_SUPPORTED 로
 * 비활성화하고 픽스처는 TransactionTemplate 으로 명시적 커밋한다. 검증은 memberId 기준 조회다.
 *
 * <p>activity 도메인 리스너는 H2 의 ActivityRecord DDL(TINYINT(1)) 파싱 한계로 제외했다
 * (MemberDismissCompletenessTest 참고).
 */
@DataJpaTest
@Import({
        MemberDismissUsecase.class,
        TeamMemberCleanupService.class,
        CommentMemberDismissListener.class,
        LetterMemberDismissListener.class,
        BadgeMemberDismissListener.class,
        NotificationMemberDismissListener.class,
        ScoreMemberDismissListener.class,
        BlockDeleteService.class,
        BlockMemberDismissListener.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberDismissRollbackTest {

    static final String BOOM = "제명 정리 강제 실패 (롤백 검증용)";

    @Autowired
    private MemberDismissUsecase memberDismissUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MemberRepository memberRepository;

    /** 이벤트 발행 이후 단계에서 터뜨려 "모든 리스너가 지운 뒤 실패"를 순서 의존 없이 재현한다 */
    @MockBean
    private CareerRepository careerRepository;

    @MockBean
    private MemberBlacklistCreateService memberBlacklistCreateService;
    @MockBean
    private MemberWithdrawService memberWithdrawService;
    @MockBean
    private PostDeleteUsecase postDeleteUsecase;
    @MockBean
    private PostLikeService postLikeService;
    @MockBean
    private ScrapService scrapService;
    @MockBean
    private CommentLikeService commentLikeService;
    @MockBean
    private CommentDeleteService commentDeleteService;

    private Long victimId;

    @BeforeEach
    void setUp() {
        given(postDeleteUsecase.deleteAllOwnedBy(anyLong())).willReturn(Collections.emptySet());
        given(careerRepository.findByMemberId(anyLong())).willThrow(new IllegalStateException(BOOM));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        this.victimId = tx.execute(status -> {
            Member victim = persistMember("victim");
            Member other = persistMember("other");

            Badge badge = new Badge("첫 게시글", null, "설명", "요건");
            entityManager.persist(badge);
            entityManager.persist(MemberBadge.create(victim, badge));

            entityManager.persist(Notification.of(victim.getId(), NotificationType.NOTICE, "{}"));

            entityManager.persist(DeviceToken.builder()
                    .memberId(victim.getId())
                    .token("token-" + System.nanoTime())
                    .platform(Platform.ANDROID)
                    .build());

            entityManager.persist(PersonalActivityScore.builder()
                    .member(victim)
                    .score(BigDecimal.valueOf(100))
                    .rewardPrefixSum(BigDecimal.ZERO)
                    .penaltyPrefixSum(BigDecimal.ZERO)
                    .build());

            entityManager.persist(Letter.create("제목", "내용", null, "reply@test.com", victim, other));

            entityManager.persist(Block.of(victim.getId(), other.getId()));

            return victim.getId();
        });
    }

    @Test
    @DisplayName("제명 정리 도중 실패하면 예외가 전파되고 배지·알림·디바이스토큰·점수·쪽지·차단·member 가 전부 롤백되어 잔존한다")
    void 정리_실패시_전체_롤백되어_아무것도_삭제되지_않는다() {
        Member victim = loadInReadTx(victimId);

        assertThatThrownBy(() -> memberDismissUsecase.dismiss(victim, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(BOOM);

        // 전체 롤백: 정상 리스너들이 먼저 지운 것까지 되돌아와 모든 데이터가 잔존해야 한다.
        assertThat(memberRepository.findById(victimId))
                .as("member row 가 롤백되어 잔존해야 한다").isPresent();
        assertThat(countBadges(victimId)).as("MemberBadge 롤백 잔존").isEqualTo(1);
        assertThat(countNotifications(victimId)).as("Notification 롤백 잔존").isEqualTo(1);
        assertThat(countDeviceTokens(victimId)).as("DeviceToken 롤백 잔존").isEqualTo(1);
        assertThat(countScores(victimId)).as("PersonalActivityScore 롤백 잔존").isEqualTo(1);
        assertThat(countLetters(victimId)).as("Letter 롤백 잔존").isEqualTo(1);
        assertThat(countBlocks(victimId)).as("Block 롤백 잔존").isEqualTo(1);
    }

    // ===== 검증 헬퍼 (memberId 기준, 새 읽기 트랜잭션) =====

    private long countBadges(Long memberId) {
        return countInReadTx("select count(mb) from MemberBadge mb where mb.member.id = :id", memberId);
    }

    private long countNotifications(Long memberId) {
        return countInReadTx("select count(n) from Notification n where n.memberId = :id", memberId);
    }

    private long countDeviceTokens(Long memberId) {
        return countInReadTx("select count(dt) from DeviceToken dt where dt.memberId = :id", memberId);
    }

    private long countScores(Long memberId) {
        return countInReadTx("select count(s) from PersonalActivityScore s where s.member.id = :id", memberId);
    }

    private long countLetters(Long memberId) {
        return countInReadTx(
                "select count(l) from Letter l where l.sender.id = :id or l.receiver.id = :id", memberId);
    }

    private long countBlocks(Long memberId) {
        return countInReadTx(
                "select count(b) from Block b where b.blockerId = :id or b.blockedId = :id", memberId);
    }

    private long countInReadTx(String jpql, Long memberId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery(jpql, Long.class)
                .setParameter("id", memberId)
                .getSingleResult());
    }

    private Member loadInReadTx(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Member.class, id));
    }

    private Member persistMember(String prefix) {
        Member member = Member.builder()
                .name("회원")
                .email(prefix + System.nanoTime() + "@test.com")
                .phoneNumber(String.valueOf(System.nanoTime()))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }
}
