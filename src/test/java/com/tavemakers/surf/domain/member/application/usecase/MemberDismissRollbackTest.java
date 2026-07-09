package com.tavemakers.surf.domain.member.application.usecase;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.badge.domain.event.BadgeMemberDismissListener;
import com.tavemakers.surf.domain.badge.domain.entity.Badge;
import com.tavemakers.surf.domain.badge.domain.entity.MemberBadge;
import com.tavemakers.surf.domain.comment.domain.event.CommentMemberDismissListener;
import com.tavemakers.surf.domain.comment.domain.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.domain.service.CommentLikeService;
import com.tavemakers.surf.domain.letter.domain.entity.Letter;
import com.tavemakers.surf.domain.letter.domain.event.LetterMemberDismissListener;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.domain.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.member.domain.repository.MemberRepository;
import com.tavemakers.surf.domain.member.domain.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.domain.service.MemberWithdrawService;
import com.tavemakers.surf.domain.notification.domain.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.domain.entity.Notification;
import com.tavemakers.surf.domain.notification.domain.entity.NotificationType;
import com.tavemakers.surf.domain.notification.domain.entity.Platform;
import com.tavemakers.surf.domain.notification.domain.event.NotificationMemberDismissListener;
import com.tavemakers.surf.domain.score.domain.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.domain.event.ScoreMemberDismissListener;
import com.tavemakers.surf.domain.post.domain.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.application.usecase.PostDeleteUsecase;
import com.tavemakers.surf.domain.scrap.domain.service.ScrapService;
import com.tavemakers.surf.domain.team.domain.service.TeamMemberCleanupService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
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
 * <p>이를 재현하기 위해 테스트 전용 실패 리스너({@link FailingListenerConfig})를 @Import 로 등록한다.
 * 이 리스너가 "가장 늦게" 실행되는 근거는 @Order 가 아니라(LOWEST_PRECEDENCE 는 무순서 리스너의
 * 기본값과 동률이라 상대 순서를 보장하지 못한다) @Import 배열에서 정상 리스너들보다 뒤에 등록되는
 * 순서다 — 정상 리스너들이 먼저 delete 를 수행한 뒤 예외가 나므로, 그 삭제까지 롤백되는지 검증하는
 * 가장 엄격한 시나리오다. 실패 리스너는 이 클래스에만 격리되어 완전성 테스트에는 영향을 주지 않는다.
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
        MemberDismissRollbackTest.FailingListenerConfig.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberDismissRollbackTest {

    static final String BOOM = "제명 리스너 강제 실패 (롤백 검증용)";

    /**
     * MemberDismissedEvent 를 마지막에 받아 예외를 던지는 테스트 전용 리스너.
     * static @TestConfiguration 이라 이 테스트 클래스 컨텍스트에만 등록되며,
     * "마지막 실행"은 @Import 배열의 등록 순서(정상 리스너들 뒤)로 확보한다.
     */
    @TestConfiguration
    static class FailingListenerConfig {
        @EventListener
        public void failOnDismiss(MemberDismissedEvent event) {
            throw new IllegalStateException(BOOM);
        }
    }

    @Autowired
    private MemberDismissUsecase memberDismissUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MemberRepository memberRepository;

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

            return victim.getId();
        });
    }

    @Test
    @DisplayName("제명 중 리스너 하나가 실패하면 예외가 전파되고 배지·알림·디바이스토큰·점수·쪽지·member 가 전부 롤백되어 잔존한다")
    void 리스너_실패시_전체_롤백되어_아무것도_삭제되지_않는다() {
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
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
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
