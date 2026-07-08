package com.tavemakers.surf.domain.member.application.usecase;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.badge.application.event.BadgeMemberDismissListener;
import com.tavemakers.surf.domain.badge.domain.entity.Badge;
import com.tavemakers.surf.domain.badge.domain.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.domain.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentMention;
import com.tavemakers.surf.domain.comment.event.CommentMemberDismissListener;
import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.event.LetterMemberDismissListener;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.domain.repository.MemberRepository;
import com.tavemakers.surf.domain.member.domain.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.domain.service.MemberWithdrawService;
import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.entity.Platform;
import com.tavemakers.surf.domain.notification.event.NotificationMemberDismissListener;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.post.PostDeleteUsecase;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.event.ScoreMemberDismissListener;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.domain.scrap.domain.service.ScrapService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 회원 제명(dismiss) 완전성 통합 테스트 (D1 — docs/refactoring-plan.md).
 *
 * <p>MemberDismissUsecase.dismiss 는 단일 @Transactional 안에서 MemberDismissedEvent 를 동기 발행하고,
 * 6개 도메인 리스너가 같은 트랜잭션에서 각자의 회원 데이터를 삭제한다. 이 테스트는 제명 회원 1명에
 * 각 도메인 데이터를 심은 뒤 dismiss 를 호출하고, <b>모든 도메인 데이터가 빠짐없이 삭제되고 member row 도
 * 사라지는지</b>를 각 repository 로 검증한다.
 *
 * <p>검증 대상은 "이벤트 → 리스너 정리" 경로이므로, 게시글/댓글/좋아요/스크랩 등 순서·데이터 의존이 큰
 * 오케스트레이션 경로(PostDeleteUsecase 체인)는 @MockBean 으로 대체한다 (아래 mock 목록 참고).
 *
 * <p>dismiss 자체가 @Transactional 이므로, 실제 커밋 결과를 검증하기 위해 클래스 트랜잭션을
 * NOT_SUPPORTED 로 비활성화하고 픽스처는 TransactionTemplate 으로 명시적 커밋한다
 * (CommentLikeConcurrencyTest 패턴). NOT_SUPPORTED 라 테스트 간 데이터가 남을 수 있으므로
 * 검증은 항상 memberId 기준으로 조회한다.
 */
@DataJpaTest
// 리스너 등록 순서를 의도적으로 "적대적 순열"로 배치한다: Score 직후에 Comment(clearAutomatically
// 벌크 삭제)가 **인접**해야 감지력이 있다 — Score 가 파생 삭제(지연 flush 큐잉)로 회귀하면 바로 뒤
// Comment 의 clear 가 큐를 취소해 이 테스트가 실패한다. 사이에 flushAutomatically 벌크 리스너가
// 끼면 큐가 미리 flush 되어 감지하지 못하므로, 인접 배치를 유지하고 알파벳순으로 "정리"하지 말 것.
@Import({
        MemberDismissUsecase.class,
        TeamMemberCleanupService.class,
        ScoreMemberDismissListener.class,
        CommentMemberDismissListener.class,
        NotificationMemberDismissListener.class,
        BadgeMemberDismissListener.class,
        LetterMemberDismissListener.class,
        // NOTE: ActivityMemberDismissListener 는 의도적으로 제외한다.
        // ActivityRecord 엔티티의 컬럼 정의(columnDefinition = "TINYINT(1) default 0")를 H2 2.x 가
        // MODE=MySQL 에서도 파싱하지 못해 activity_record 테이블 DDL 생성이 실패한다(운영 MySQL 에서는 정상).
        // 리스너를 등록하면 dismiss 가 존재하지 않는 테이블을 조회하다 실패하므로, activity 도메인은
        // 이 완전성 테스트의 검증 범위에서 제외했다 (테스트 인프라 한계이며 프로덕션 버그 아님).
})
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → dismiss 의 실제 커밋 검증
class MemberDismissCompletenessTest {

    @Autowired
    private MemberDismissUsecase memberDismissUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 검증용 repository (실제 빈)
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberBadgeRepository memberBadgeRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private PersonalActivityScoreRepository personalActivityScoreRepository;

    // 순서·데이터 의존 오케스트레이션 경로는 mock — 검증 대상은 이벤트→리스너 정리 경로다.
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
        // deleteAllOwnedBy 는 Set<Long> 을 반환하고 그 값이 commentDeleteService 로 전달되므로 NPE 방지용 stub.
        given(postDeleteUsecase.deleteAllOwnedBy(anyLong())).willReturn(Collections.emptySet());

        // 클래스 트랜잭션(NOT_SUPPORTED)이 없으므로 픽스처는 별도 트랜잭션으로 명시적 커밋한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        this.victimId = tx.execute(status -> {
            Member victim = persistMember("victim");
            Member other = persistMember("other"); // 쪽지 상대 / 게시글·댓글 작성자

            // 1) 배지 보유 (Badge + MemberBadge)
            Badge badge = new Badge("첫 게시글", null, "설명", "요건");
            entityManager.persist(badge);
            entityManager.persist(MemberBadge.create(victim, badge));

            // 2) 알림
            entityManager.persist(Notification.of(victim.getId(), NotificationType.NOTICE, "{}"));

            // 3) 디바이스 토큰
            entityManager.persist(DeviceToken.builder()
                    .memberId(victim.getId())
                    .token("token-" + System.nanoTime())
                    .platform(Platform.ANDROID)
                    .build());

            // 4) 개인 활동 점수
            entityManager.persist(PersonalActivityScore.builder()
                    .member(victim)
                    .score(BigDecimal.valueOf(100))
                    .rewardPrefixSum(BigDecimal.ZERO)
                    .penaltyPrefixSum(BigDecimal.ZERO)
                    .build());

            // 5) 쪽지 (제명 대상이 보낸 것 + 받은 것)
            entityManager.persist(Letter.create("제목", "내용", null, "reply@test.com", victim, other));
            entityManager.persist(Letter.create("제목2", "내용2", null, "reply@test.com", other, victim));

            // 6) 댓글 멘션 — 타인(other) 게시글에 타인(other)이 단 댓글에서 제명 대상(victim)을 멘션
            Board board = Board.builder().name("자유게시판").type(BoardType.GENERAL).build();
            entityManager.persist(board);
            BoardCategory category = BoardCategory.builder()
                    .board(board).name("잡담").slug("chat").build();
            entityManager.persist(category);
            Post post = Post.builder()
                    .title("제목").content("내용")
                    .board(board).boardName(board.getName())
                    .category(category).categoryName(category.getName())
                    .member(other)
                    .build();
            entityManager.persist(post);
            Comment comment = Comment.root(post, other, "@victim 안녕");
            entityManager.persist(comment);
            entityManager.persist(CommentMention.of(comment, victim));

            return victim.getId();
        });

        // 사전 조건: 모든 도메인 데이터가 1건 이상 존재
        assertThat(memberRepository.findById(victimId)).isPresent();
        assertThat(countBadges(victimId)).isEqualTo(1);
        assertThat(countNotifications(victimId)).isEqualTo(1);
        assertThat(countDeviceTokens(victimId)).isEqualTo(1);
        assertThat(countScores(victimId)).isEqualTo(1);
        assertThat(countLetters(victimId)).isEqualTo(2);
        assertThat(countMentions(victimId)).isEqualTo(1);
    }

    @Test
    @DisplayName("APPROVED 회원 제명 시 배지·알림·디바이스토큰·점수·쪽지·멘션이 모두 삭제되고 member row 도 사라진다")
    void 제명하면_전_도메인_데이터가_빠짐없이_삭제된다() {
        Member victim = loadInReadTx(victimId);

        memberDismissUsecase.dismiss(victim, 999L);

        assertThat(memberRepository.findById(victimId))
                .as("member row 가 삭제되어야 한다").isEmpty();
        assertThat(countBadges(victimId)).as("MemberBadge 잔존").isZero();
        assertThat(countNotifications(victimId)).as("Notification 잔존").isZero();
        assertThat(countDeviceTokens(victimId)).as("DeviceToken 잔존").isZero();
        assertThat(countScores(victimId)).as("PersonalActivityScore 잔존").isZero();
        assertThat(countLetters(victimId)).as("Letter 잔존").isZero();
        assertThat(countMentions(victimId)).as("CommentMention 잔존").isZero();
    }

    // ===== 검증 헬퍼: 새 읽기 트랜잭션에서 memberId 기준 count (NOT_SUPPORTED 라 다른 테스트 데이터와 섞이지 않도록) =====

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

    private long countMentions(Long memberId) {
        return countInReadTx(
                "select count(cm) from CommentMention cm where cm.mentionedMember.id = :id", memberId);
    }

    private long countInReadTx(String jpql, Long memberId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery(jpql, Long.class)
                .setParameter("id", memberId)
                .getSingleResult());
    }

    /** dismiss 인자로 넘길 detached Member 를 새 읽기 트랜잭션에서 로드한다. */
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
