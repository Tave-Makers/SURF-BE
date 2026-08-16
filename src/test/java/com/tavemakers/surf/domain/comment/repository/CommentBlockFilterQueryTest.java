package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 댓글·대댓글 차단 필터 JPQL 실검증 (이슈 #370).
 *
 * <p>핵심은 <b>목록과 totalCount 가 같은 기준으로 줄어드는지</b>다. 한쪽만 필터링하면
 * "댓글 3개"라고 표시되는데 목록에는 아무것도 없는 화면이 나온다.
 *
 * <p>대댓글은 별도 테이블이 아니라 같은 Slice 의 depth>0 행이므로 같은 쿼리로 함께 걸러져야 한다.
 */
@DataJpaTest
class CommentBlockFilterQueryTest {

    /** 차단이 0건일 때 BlockGetService 가 넣어주는 값 */
    private static final Set<Long> NO_ONE_BLOCKED = Set.of(-1L);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager em;

    private Post post;
    private Member blockedAuthor;
    private Member normalAuthor;

    @BeforeEach
    void setUp() {
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        em.persist(board);
        BoardCategory category = BoardCategory.of(board, "잡담", "chat-" + System.nanoTime());
        em.persist(category);

        blockedAuthor = persistMember("blocked");
        normalAuthor = persistMember("normal");

        post = Post.of("제목", "본문", false, false, false, board, category, normalAuthor);
        em.persist(post);
    }

    @Test
    @DisplayName("차단 작성자의 댓글이 목록과 totalCount 에서 함께 빠진다")
    void 목록과_카운트가_함께_줄어든다() {
        em.persist(Comment.root(post, blockedAuthor, "차단 댓글"));
        em.persist(Comment.root(post, normalAuthor, "정상 댓글"));
        em.flush();

        Set<Long> excluded = Set.of(blockedAuthor.getId());
        Slice<Comment> slice = commentRepository.findByPostIdExcludingAuthors(
                post.getId(), excluded, PageRequest.of(0, 20));
        long totalCount = commentRepository.countByPostIdExcludingAuthors(post.getId(), excluded);

        assertThat(slice.getContent()).extracting(Comment::getContent).containsExactly("정상 댓글");
        assertThat(totalCount)
                .as("목록만 줄고 카운트가 그대로면 '댓글 2개'인데 1개만 보이는 화면이 된다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("차단 작성자의 대댓글도 함께 빠진다 — 같은 Slice 의 depth>0 행이다")
    void 대댓글도_함께_빠진다() {
        Comment root = Comment.root(post, normalAuthor, "루트 댓글");
        em.persist(root);
        em.persist(Comment.child(post, blockedAuthor, "차단 대댓글", root));
        em.persist(Comment.child(post, normalAuthor, "정상 대댓글", root));
        em.flush();

        Set<Long> excluded = Set.of(blockedAuthor.getId());
        Slice<Comment> slice = commentRepository.findByPostIdExcludingAuthors(
                post.getId(), excluded, PageRequest.of(0, 20));

        assertThat(slice.getContent()).extracting(Comment::getContent)
                .containsExactlyInAnyOrder("루트 댓글", "정상 대댓글");
        assertThat(commentRepository.countByPostIdExcludingAuthors(post.getId(), excluded)).isEqualTo(2);
    }

    @Test
    @DisplayName("차단이 없으면(sentinel) 아무 댓글도 제외되지 않는다")
    void sentinel이면_아무도_제외되지_않는다() {
        em.persist(Comment.root(post, blockedAuthor, "댓글1"));
        em.persist(Comment.root(post, normalAuthor, "댓글2"));
        em.flush();

        Slice<Comment> slice = commentRepository.findByPostIdExcludingAuthors(
                post.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        assertThat(slice.getContent()).hasSize(2);
        assertThat(commentRepository.countByPostIdExcludingAuthors(post.getId(), NO_ONE_BLOCKED)).isEqualTo(2);
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
        em.persist(member);
        return member;
    }
}
