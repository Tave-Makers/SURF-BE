package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.support.ConcurrencyTestHelper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 댓글 좋아요 카운트 lost update 재현 테스트.
 *
 * Comment 엔티티에 @Version 낙관적 락이 없어, 서로 다른 회원 N명이 동시에 같은 댓글에
 * toggleLike 를 호출하면 likeCount++ 가 서로 덮어써져 최종 likeCount < N 이 된다.
 *
 * 각 스레드가 독립 트랜잭션으로 커밋해야 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED 로 비활성화하고,
 * CommentLikeService 를 스프링 프록시 빈으로 등록해 메서드 @Transactional 이 스레드별로 동작하게 한다.
 */
@DataJpaTest
@Import(CommentLikeService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class CommentLikeConcurrencyTest {

    private static final int CONCURRENT_MEMBERS = 10;

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // toggleLike 의 부수효과(알림/로그) 및 타 도메인 조회는 mock. 검증 대상은 likeCount 정합성뿐이다.
    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private PostGetService postGetService;
    @MockBean
    private LogEventEmitter logEventEmitter;
    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private Long commentId;
    private Long postId;
    private List<Long> memberIds;

    @BeforeEach
    void setUp() {
        // 클래스 트랜잭션(NOT_SUPPORTED)이 없으므로, 준비 데이터는 별도 트랜잭션으로 명시적 커밋한다.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Board board = Board.builder().name("자유게시판").type(BoardType.GENERAL).build();
            entityManager.persist(board);

            BoardCategory category = BoardCategory.builder()
                    .board(board).name("잡담").slug("chat").build();
            entityManager.persist(category);

            // 클래스 트랜잭션이 없어 테스트 간 데이터가 남으므로, 이메일은 실행마다 고유하게 만든다 (unique 충돌 방지)
            Member author = persistMember("author" + System.nanoTime() + "@test.com");

            Post post = Post.builder()
                    .title("제목").content("내용")
                    .board(board).boardName(board.getName())
                    .category(category).categoryName(category.getName())
                    .member(author)
                    .build();
            entityManager.persist(post);

            Comment comment = Comment.root(post, author, "댓글");
            entityManager.persist(comment);

            memberIds = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_MEMBERS; i++) {
                Member m = persistMember("liker" + i + "_" + System.nanoTime() + "@test.com");
                memberIds.add(m.getId());
            }

            this.commentId = comment.getId();
            this.postId = post.getId();
        });

        // toggleLike 내부의 타 도메인 조회는 mock 처리. 각 스레드 트랜잭션에서 필요한 엔티티를 다시 로드해 반환한다.
        given(memberGetService.getMember(org.mockito.ArgumentMatchers.anyLong()))
                .willAnswer(inv -> loadInReadTx(Member.class, inv.getArgument(0)));
        given(postGetService.getPost(org.mockito.ArgumentMatchers.anyLong()))
                .willAnswer(inv -> loadPostInReadTx(inv.getArgument(0)));
    }

    /** mock 응답용: 새 읽기 트랜잭션에서 엔티티를 로드해 detached 로 반환한다. */
    private <T> T loadInReadTx(Class<T> type, Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(type, id));
    }

    /** mock 응답용: post + board 를 함께 초기화해 detached 로 반환한다. */
    private Post loadPostInReadTx(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Post post = entityManager.find(Post.class, id);
            post.getBoard().getId(); // createNotificationAtCommentLike 에서 접근 → 초기화
            return post;
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
    @DisplayName("서로 다른 회원 10명이 동시에 같은 댓글에 좋아요를 누르면 likeCount 는 10이어야 한다")
    void 동시_좋아요_10명이면_likeCount는_10() throws InterruptedException {
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_MEMBERS,
                new MemberPerThreadTask(memberIds)
        );

        Long likeCount = loadInReadTx(Comment.class, commentId).getLikeCount();

        assertThat(result.failureCount()).isZero();
        assertThat(likeCount)
                .as("동시 좋아요 %d건이 lost update 없이 반영되어야 한다", CONCURRENT_MEMBERS)
                .isEqualTo((long) CONCURRENT_MEMBERS);
    }

    @Test
    @DisplayName("같은 회원이 동시에 좋아요를 중복 등록하면 도메인 예외로 처리되고 카운트·row 정합성이 유지된다")
    void 동일_회원_동시_중복_등록시_도메인_예외와_정합성_유지() throws InterruptedException {
        Long memberId = memberIds.get(0);

        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                5,
                () -> commentLikeService.toggleLike(commentId, memberId)
        );

        // 토글 특성상 인터리빙에 따라 최종 상태는 좋아요(1) 또는 취소(0) 둘 다 가능하다.
        // 불변식: ① 실패는 전부 도메인 예외(CommentLikeAlreadyExistsException)여야 하고
        //         (DataIntegrityViolationException/UnexpectedRollbackException 이 새면 회귀)
        //         ② likeCount 와 CommentLike row 수는 항상 일치하며 0 또는 1 이어야 한다.
        assertThat(result.failures())
                .allSatisfy(t -> assertThat(t).isInstanceOf(CommentLikeAlreadyExistsException.class));

        Long likeCount = loadInReadTx(Comment.class, commentId).getLikeCount();
        Long rowCount = countLikeRowsInReadTx();

        assertThat(likeCount).isEqualTo(rowCount);
        assertThat(likeCount).isBetween(0L, 1L);
    }

    /** CommentLike row 수를 새 읽기 트랜잭션에서 조회한다. */
    private Long countLikeRowsInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(cl) from CommentLike cl where cl.comment.id = :cid", Long.class)
                .setParameter("cid", commentId)
                .getSingleResult());
    }

    /** 스레드마다 서로 다른 memberId 로 toggleLike 를 호출하도록 분배한다. */
    private final class MemberPerThreadTask implements Runnable {
        private final List<Long> ids;
        private final java.util.concurrent.atomic.AtomicInteger cursor =
                new java.util.concurrent.atomic.AtomicInteger();

        private MemberPerThreadTask(List<Long> ids) {
            this.ids = ids;
        }

        @Override
        public void run() {
            Long memberId = ids.get(cursor.getAndIncrement());
            commentLikeService.toggleLike(commentId, memberId);
        }
    }
}
