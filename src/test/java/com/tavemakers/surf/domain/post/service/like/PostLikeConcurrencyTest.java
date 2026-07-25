package com.tavemakers.surf.domain.post.service.like;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostLikeAlreadyExistsException;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 게시글 좋아요 카운트 동시성/멱등성 회귀 테스트.
 *
 * <p>수정 전 문제:
 * <ol>
 *   <li>like/unlike가 version-CAS UPDATE를 3회 재시도하고 소진 시 OptimisticLockException(500).
 *       고경합 게시글에서 사용자에게 500이 노출됐다.</li>
 *   <li>like의 save 후 UK 충돌을 catch하고 정상 리턴 — flush 시점 예외로 트랜잭션이 이미
 *       rollback-only라 커밋 단계에서 UnexpectedRollbackException(500)이 됐다.</li>
 * </ol>
 *
 * <p>수정 후 기대 동작: 카운트는 무조건 원자적 UPDATE(재시도·버전 없음)로 증감하고,
 * 동시 중복 like의 패자는 도메인 예외(PostLikeAlreadyExistsException, 409)로 정직하게 전파된다.
 *
 * <p>각 스레드가 독립 트랜잭션으로 커밋해야 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED로 비활성화하고,
 * PostLikeService를 스프링 프록시 빈으로 등록해 메서드 @Transactional이 스레드별로 동작하게 한다.
 */
@DataJpaTest
@Import(PostLikeService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class PostLikeConcurrencyTest {

    private static final int CONCURRENT_MEMBERS = 10;

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // like의 부수효과(알림 이벤트)와 타 도메인 조회는 mock. 검증 대상은 likeCount·row 정합성뿐이다.
    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private ApplicationEventPublisher eventPublisher;

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

            memberIds = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_MEMBERS; i++) {
                Member m = persistMember("liker" + i + "_" + System.nanoTime() + "@test.com");
                memberIds.add(m.getId());
            }

            this.postId = post.getId();
        });

        // like 내부의 타 도메인 조회는 mock 처리. 각 스레드 트랜잭션에서 필요한 엔티티를 다시 로드해 반환한다.
        given(memberGetService.getMember(anyLong()))
                .willAnswer(inv -> loadInReadTx(Member.class, inv.getArgument(0)));
    }

    /** mock 응답용: 새 읽기 트랜잭션에서 엔티티를 로드해 detached로 반환한다. */
    private <T> T loadInReadTx(Class<T> type, Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(type, id));
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
    @DisplayName("서로 다른 회원 10명이 동시에 같은 게시글에 좋아요를 누르면 likeCount는 10이어야 한다")
    void 동시_좋아요_10명이면_likeCount는_10() throws InterruptedException {
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_MEMBERS,
                new MemberPerThreadTask(memberIds, id -> postLikeService.like(postId, id))
        );

        // 수정 전에는 version-CAS 재시도 소진으로 OptimisticLockException(500)이 발생했다.
        assertThat(result.failureCount())
                .as("원자적 UPDATE로 전환 후 고경합에서도 실패(재시도 소진 500)가 없어야 한다")
                .isZero();
        assertThat(loadLikeCountInReadTx())
                .as("동시 좋아요 %d건이 lost update 없이 반영되어야 한다", CONCURRENT_MEMBERS)
                .isEqualTo((long) CONCURRENT_MEMBERS);
    }

    @Test
    @DisplayName("같은 회원이 동시에 좋아요를 중복 등록하면 도메인 예외로 처리되고 카운트·row 정합성이 유지된다")
    void 동일_회원_동시_중복_등록시_도메인_예외와_정합성_유지() throws InterruptedException {
        Long memberId = memberIds.get(0);

        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                5,
                () -> postLikeService.like(postId, memberId)
        );

        // 불변식: ① race 패자는 사전 exists 체크로 멱등 종료(성공)하거나 도메인 예외(409)여야 한다.
        //           (DataIntegrityViolationException/UnexpectedRollbackException이 새면 회귀)
        //         ② likeCount와 PostLike row 수는 일치하며 정확히 1이어야 한다.
        assertThat(result.failures())
                .allSatisfy(t -> assertThat(t).isInstanceOf(PostLikeAlreadyExistsException.class));

        assertThat(countLikeRowsInReadTx())
                .as("unique 제약으로 PostLike row는 정확히 1개여야 한다")
                .isEqualTo(1L);
        assertThat(loadLikeCountInReadTx())
                .as("커밋에 성공한 좋아요 1건만 likeCount를 증가시켜 1이어야 한다")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 10건 후 10명이 동시에 취소하면 likeCount는 0이고 음수가 되지 않는다")
    void 동시_좋아요_취소시_likeCount는_0() throws InterruptedException {
        for (Long memberId : memberIds) {
            postLikeService.like(postId, memberId);
        }
        assertThat(loadLikeCountInReadTx()).isEqualTo((long) CONCURRENT_MEMBERS);

        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_MEMBERS,
                new MemberPerThreadTask(memberIds, id -> postLikeService.unlike(postId, id))
        );

        assertThat(result.failureCount())
                .as("원자적 UPDATE로 전환 후 동시 취소에서도 실패(재시도 소진 500)가 없어야 한다")
                .isZero();
        assertThat(loadLikeCountInReadTx())
                .as("취소 %d건이 전부 반영되고 음수가 되지 않아야 한다", CONCURRENT_MEMBERS)
                .isEqualTo(0L);
    }

    /** Post.likeCount를 새 읽기 트랜잭션에서 조회한다. */
    private Long loadLikeCountInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Post.class, postId).getLikeCount());
    }

    /** PostLike row 수를 새 읽기 트랜잭션에서 조회한다. */
    private Long countLikeRowsInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(pl) from PostLike pl where pl.post.id = :pid", Long.class)
                .setParameter("pid", postId)
                .getSingleResult());
    }

    /** 스레드마다 서로 다른 memberId로 작업을 호출하도록 분배한다. */
    private static final class MemberPerThreadTask implements Runnable {
        private final List<Long> ids;
        private final java.util.function.Consumer<Long> action;
        private final AtomicInteger cursor = new AtomicInteger();

        private MemberPerThreadTask(List<Long> ids, java.util.function.Consumer<Long> action) {
            this.ids = ids;
            this.action = action;
        }

        @Override
        public void run() {
            action.accept(ids.get(cursor.getAndIncrement()));
        }
    }
}
