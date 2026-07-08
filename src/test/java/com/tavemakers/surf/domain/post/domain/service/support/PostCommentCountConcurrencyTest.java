package com.tavemakers.surf.domain.post.domain.service.support;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.domain.entity.Post;
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
 * 게시글 댓글 수(commentCount) lost update 재현/회귀 테스트.
 *
 * 과거에는 Post.commentCount를 엔티티 메모리에서 증감(post.increaseCommentCount())했기에,
 * 서로 다른 회원 N명이 동시에 같은 게시글에 댓글을 작성하면 read-modify-write 사이 갱신이
 * 서로 덮어써져 최종 commentCount < N 이 됐다.
 * 현재는 PostRepository.increaseCommentCount 원자적 UPDATE(commentCount = commentCount + 1)로
 * 처리하므로 동시 N건이 손실 없이 반영되어야 한다.
 *
 * 각 스레드가 독립 트랜잭션으로 커밋해야 재현되므로 클래스 트랜잭션을 NOT_SUPPORTED로 비활성화하고,
 * PostCommentCountService를 스프링 프록시 빈으로 등록해 increase가 스레드별로 동작하게 한다.
 */
@DataJpaTest
@Import(PostCommentCountService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 제거 → 스레드별 독립 커밋
class PostCommentCountConcurrencyTest {

    private static final int CONCURRENT_COMMENTS = 10;

    @Autowired
    private PostCommentCountService postCommentCountService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long postId;

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

            this.postId = post.getId();
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
    @DisplayName("서로 다른 회원 10명이 동시에 같은 게시글에 댓글을 작성하면 commentCount는 10이어야 한다")
    void 동시_댓글_10건이면_commentCount는_10() throws InterruptedException {
        // PostCommentCountService.increase는 @Transactional이 없고, 실제로는 호출부(CommentService.createComment)의
        // 트랜잭션 경계 안에서 실행된다. @Modifying UPDATE는 활성 트랜잭션을 요구하므로 스레드마다 독립 트랜잭션으로 감싼다.
        ConcurrencyTestHelper.Result result = ConcurrencyTestHelper.runConcurrently(
                CONCURRENT_COMMENTS,
                () -> new TransactionTemplate(transactionManager)
                        .executeWithoutResult(status -> postCommentCountService.increase(postId))
        );

        Long commentCount = loadCommentCountInReadTx();

        assertThat(result.failureCount()).isZero();
        assertThat(commentCount)
                .as("동시 댓글 %d건이 lost update 없이 반영되어야 한다", CONCURRENT_COMMENTS)
                .isEqualTo((long) CONCURRENT_COMMENTS);
    }

    /** Post.commentCount를 새 읽기 트랜잭션에서 조회한다. */
    private Long loadCommentCountInReadTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager.find(Post.class, postId).getCommentCount());
    }
}
