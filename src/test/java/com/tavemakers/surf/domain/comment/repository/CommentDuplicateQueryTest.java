package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * existsRecentDuplicate JPQL 실검증 — 댓글 더블 클릭/재시도 중복 방지 쿼리.
 *
 * <p>버그 재현 배경: 동일 요청이 연달아 들어오면 댓글이 2개 저장됐다.
 * 이 쿼리가 (게시글, 작성자, 부모, 내용, 시간창) 기준으로 직전 중복을 감지한다.
 */
@DataJpaTest
class CommentDuplicateQueryTest {

    private static final int WINDOW_SECONDS = 5;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager em;

    private Member author;
    private Post post;
    private Comment rootComment;

    @BeforeEach
    void setUp() {
        author = persistMember("author");
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        em.persist(board);
        BoardCategory category = BoardCategory.of(board, "잡담", "chat");
        em.persist(category);
        post = Post.of("제목", "내용", false, false, false, board, category, author);
        em.persist(post);

        rootComment = commentRepository.save(Comment.root(post, author, "같은 내용"));
        em.flush();
    }

    private LocalDateTime windowStart() {
        return LocalDateTime.now().minusSeconds(WINDOW_SECONDS);
    }

    @Test
    @DisplayName("같은 작성자·게시글·부모(루트)·내용이 시간창 안에 있으면 true")
    void 동일_루트_댓글은_중복이다() {
        assertThat(commentRepository.existsRecentDuplicate(
                post.getId(), author.getId(), null, "같은 내용", windowStart()))
                .isTrue();
    }

    @Test
    @DisplayName("내용이 다르면 중복이 아니다")
    void 다른_내용은_중복이_아니다() {
        assertThat(commentRepository.existsRecentDuplicate(
                post.getId(), author.getId(), null, "다른 내용", windowStart()))
                .isFalse();
    }

    @Test
    @DisplayName("같은 내용이라도 부모가 다르면(루트 vs 대댓글) 중복이 아니다")
    void 부모가_다르면_중복이_아니다() {
        // 루트에 "같은 내용"이 있어도, 그 루트의 대댓글로는 중복이 아님
        assertThat(commentRepository.existsRecentDuplicate(
                post.getId(), author.getId(), rootComment.getId(), "같은 내용", windowStart()))
                .isFalse();

        // 대댓글로 저장하면 그 부모 기준으로는 중복
        commentRepository.save(Comment.child(post, author, "같은 내용", rootComment));
        em.flush();
        assertThat(commentRepository.existsRecentDuplicate(
                post.getId(), author.getId(), rootComment.getId(), "같은 내용", windowStart()))
                .isTrue();
    }

    @Test
    @DisplayName("시간창 밖(오래된 동일 댓글)은 중복이 아니다 — 정당한 재작성 허용")
    void 시간창_밖이면_중복이_아니다() {
        em.createQuery("update Comment c set c.createdAt = :past where c.id = :id")
                .setParameter("past", LocalDateTime.now().minusSeconds(WINDOW_SECONDS * 10))
                .setParameter("id", rootComment.getId())
                .executeUpdate();
        em.clear();

        assertThat(commentRepository.existsRecentDuplicate(
                post.getId(), author.getId(), null, "같은 내용", windowStart()))
                .isFalse();
    }

    private Member persistMember(String prefix) {
        long seed = System.nanoTime();
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(prefix + seed)
                .kakaoId(seed)
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
