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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 본문 수정 트랜잭션이 동시 좋아요 카운트를 덮어쓰지 않는지 검증하는 회귀 테스트 (리뷰 B-1).
 *
 * <p>배경: 좋아요/스크랩/조회수 카운트를 version-CAS에서 무조건 원자적 UPDATE로 전환하면서
 * 카운트 UPDATE가 더 이상 {@code @Version}을 올리지 않게 됐다. 이때 {@code Post}에
 * {@code @DynamicUpdate}가 없으면, 본문 수정 트랜잭션의 dirty checking flush가 <b>전체 컬럼
 * 정적 UPDATE</b>를 생성해 로드 시점 스냅샷값으로 like_count/scrap_count/view_count를
 * 조용히 덮어쓴다(예외 없이 유실). version-CAS 시절에는 최소한 낙관 락 실패로 감지됐던 충돌이
 * 무감지 데이터 유실로 바뀌는 회귀다.
 *
 * <p>재현: (a) 편집 트랜잭션이 post를 로드(likeCount=0 스냅샷)하고 필드를 수정하되 커밋 보류 →
 * (b) 별도 트랜잭션에서 좋아요가 원자적으로 카운트를 1로 올리고 커밋 →
 * (c) 편집 트랜잭션 커밋(flush). {@code @DynamicUpdate}가 없으면 (c)의 UPDATE가 like_count를
 * 0으로 되돌린다. 있으면 변경된 컬럼만 UPDATE되어 1이 보존된다.
 *
 * <p>PostLikeConcurrencyTest 와 동일 픽스처 규약: 클래스 트랜잭션 NOT_SUPPORTED + 스레드별 독립 커밋.
 */
@DataJpaTest
@Import(PostLikeService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostBodyEditCountPreservationTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private Long postId;
    private Long likerId;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Board board = Board.builder().name("자유게시판").type(BoardType.GENERAL).build();
            entityManager.persist(board);

            BoardCategory category = BoardCategory.builder()
                    .board(board).name("잡담").slug("chat").build();
            entityManager.persist(category);

            Member author = persistMember("author" + System.nanoTime() + "@test.com");

            Post post = Post.builder()
                    .title("제목").content("내용")
                    .board(board).boardName(board.getName())
                    .category(category).categoryName(category.getName())
                    .member(author)
                    .build();
            entityManager.persist(post);

            Member liker = persistMember("liker" + System.nanoTime() + "@test.com");

            this.postId = post.getId();
            this.likerId = liker.getId();
        });

        given(memberGetService.getMember(anyLong()))
                .willAnswer(inv -> loadInReadTx(Member.class, inv.getArgument(0)));
    }

    @Test
    @DisplayName("본문 수정 트랜잭션이 커밋될 때, 그 사이 동시에 증가한 좋아요 카운트를 덮어쓰지 않는다")
    void 본문_수정_flush가_동시_좋아요_카운트를_덮어쓰지_않는다() throws InterruptedException {
        // (a) 편집 트랜잭션 시작 — post 로드(likeCount=0 스냅샷) 후 필드 수정, 커밋 보류
        TransactionStatus editTx = transactionManager.getTransaction(new DefaultTransactionDefinition());
        Post editing = entityManager.find(Post.class, postId);
        assertThat(editing.getLikeCount()).as("편집 시작 시점 likeCount 스냅샷은 0").isZero();
        editing.changeHasSchedule(true); // 단일 필드 dirty → 커밋 시 UPDATE 유발

        // (b) 별도 스레드의 독립 트랜잭션에서 좋아요 → 원자적으로 likeCount = 1, 커밋
        Thread liker = new Thread(() -> {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.executeWithoutResult(s -> postLikeService.like(postId, likerId));
        });
        liker.start();
        liker.join();
        assertThat(loadLikeCountInReadTx()).as("좋아요가 편집 커밋 전에 DB에 반영").isEqualTo(1L);

        // (c) 편집 트랜잭션 커밋 → dirty checking flush
        transactionManager.commit(editTx);

        // (d) @DynamicUpdate 제거 시: 전체 컬럼 정적 UPDATE가 like_count를 스냅샷(0)으로 덮어써 0이 된다(회귀).
        //     @DynamicUpdate 유지 시: 변경 컬럼(has_schedule)만 UPDATE → like_count = 1 보존.
        assertThat(loadLikeCountInReadTx())
                .as("본문 수정 flush가 동시 좋아요 카운트를 덮어쓰지 않아야 한다 (@DynamicUpdate)")
                .isEqualTo(1L);
    }

    /**
     * likeCount를 항상 독립 트랜잭션(REQUIRES_NEW)에서 조회한다.
     * 편집 트랜잭션이 열려 있는 동안 기본 REQUIRED로 읽으면 열린 트랜잭션에 합류해
     * 영속성 컨텍스트에 캐시된 스냅샷을 반환하므로, 별도로 커밋된 DB 값을 보려면 트랜잭션을 분리해야 한다.
     */
    private Long loadLikeCountInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> entityManager.find(Post.class, postId).getLikeCount());
    }

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
}
