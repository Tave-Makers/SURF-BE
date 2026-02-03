package com.tavemakers.surf.domain.post.entity;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostContentLongTextTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager em;

    private Member member;
    private Board board;
    private BoardCategory category;

    @BeforeEach
    void setUp() {
        // Member 생성
        member = Member.builder()
                .kakaoId(123456789L)
                .name("테스트유저")
                .email("test@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        em.persist(member);

        // Board 생성 (type 필수)
        board = Board.builder()
                .name("테스트게시판")
                .type(BoardType.NOTICE)
                .build();
        em.persist(board);

        // BoardCategory 생성 (slug 필수)
        category = BoardCategory.builder()
                .name("테스트카테고리")
                .slug("test-category")
                .board(board)
                .build();
        em.persist(category);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("255자 이상의 긴 본문 저장 테스트")
    void savePostWithLongContent() {
        // given: 1000자 이상의 긴 본문 생성
        String longContent = "가".repeat(1000);
        assertThat(longContent.length()).isEqualTo(1000);

        Post post = Post.builder()
                .title("테스트 제목")
                .content(longContent)
                .postedAt(LocalDateTime.now())
                .board(em.find(Board.class, board.getId()))
                .boardName(board.getName())
                .category(em.find(BoardCategory.class, category.getId()))
                .categoryName(category.getName())
                .member(em.find(Member.class, member.getId()))
                .scrapCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .isReserved(false)
                .hasSchedule(false)
                .viewCount(0)
                .build();

        // when
        Post savedPost = postRepository.save(post);
        em.flush();
        em.clear();

        // then
        Post foundPost = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(foundPost.getContent()).isEqualTo(longContent);
        assertThat(foundPost.getContent().length()).isEqualTo(1000);
    }

    @Test
    @DisplayName("10,000자 긴 본문 저장 테스트")
    void savePostWith10000CharContent() {
        // given: 10,000자의 매우 긴 본문
        String veryLongContent = "테스트내용입니다.".repeat(1000);
        assertThat(veryLongContent.length()).isGreaterThan(5000);

        Post post = Post.builder()
                .title("매우 긴 본문 테스트")
                .content(veryLongContent)
                .postedAt(LocalDateTime.now())
                .board(em.find(Board.class, board.getId()))
                .boardName(board.getName())
                .category(em.find(BoardCategory.class, category.getId()))
                .categoryName(category.getName())
                .member(em.find(Member.class, member.getId()))
                .scrapCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .isReserved(false)
                .hasSchedule(false)
                .viewCount(0)
                .build();

        // when
        Post savedPost = postRepository.save(post);
        em.flush();
        em.clear();

        // then
        Post foundPost = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(foundPost.getContent()).isEqualTo(veryLongContent);
    }

    @Test
    @DisplayName("100,000자 초대형 본문 저장 테스트 (LONGTEXT 검증)")
    void savePostWith100000CharContent() {
        // given: 100,000자의 초대형 본문 (VARCHAR로는 불가능한 크기)
        String hugeContent = "A".repeat(100_000);
        assertThat(hugeContent.length()).isEqualTo(100_000);

        Post post = Post.builder()
                .title("초대형 본문 테스트")
                .content(hugeContent)
                .postedAt(LocalDateTime.now())
                .board(em.find(Board.class, board.getId()))
                .boardName(board.getName())
                .category(em.find(BoardCategory.class, category.getId()))
                .categoryName(category.getName())
                .member(em.find(Member.class, member.getId()))
                .scrapCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .isReserved(false)
                .hasSchedule(false)
                .viewCount(0)
                .build();

        // when
        Post savedPost = postRepository.save(post);
        em.flush();
        em.clear();

        // then
        Post foundPost = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(foundPost.getContent()).isEqualTo(hugeContent);
        assertThat(foundPost.getContent().length()).isEqualTo(100_000);
    }
}
