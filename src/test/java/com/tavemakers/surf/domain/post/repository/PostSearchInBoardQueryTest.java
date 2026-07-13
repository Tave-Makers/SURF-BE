package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * searchInBoard JPQL 실검증 — 게시판 내 검색 필터.
 *
 * <p>버그 재현 배경: 게시판 안에서 검색해도 전체 게시판이 통합 검색됐다(boardId 필터 부재).
 * searchInBoard 는 지정 게시판의 게시글만 반환해야 한다.
 */
@DataJpaTest
class PostSearchInBoardQueryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager em;

    private Board boardA;
    private Board boardB;

    @BeforeEach
    void setUp() {
        Member writer = persistMember("writer");
        boardA = persistBoard("자유게시판");
        boardB = persistBoard("정보게시판");

        persistPost(boardA, writer, "서핑 일지", "본문");
        persistPost(boardB, writer, "서핑 모임 공지", "본문");
        persistPost(boardB, writer, "무관한 글", "서핑 후기 포함 본문");
        em.flush();
    }

    @Test
    @DisplayName("boardId를 지정하면 해당 게시판의 게시글만 검색된다")
    void 게시판_내_검색은_해당_게시판만_반환한다() {
        Slice<Post> result = postRepository.searchInBoard(boardA.getId(), "서핑", PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(p -> p.getBoard().getId())
                .containsOnly(boardA.getId());
        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("서핑 일지");
    }

    @Test
    @DisplayName("제목뿐 아니라 내용 매칭도 게시판 범위 안에서 검색된다")
    void 내용_매칭도_게시판_범위를_지킨다() {
        Slice<Post> result = postRepository.searchInBoard(boardB.getId(), "서핑", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2); // 제목 매칭 1 + 내용 매칭 1
        assertThat(result.getContent())
                .extracting(p -> p.getBoard().getId())
                .containsOnly(boardB.getId());
    }

    @Test
    @DisplayName("대조: 통합 검색은 전체 게시판에서 검색된다 (기존 동작 보존)")
    void 통합_검색은_전체를_반환한다() {
        Slice<Post> result = postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase("서핑", "서핑", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(3);
    }

    private Board persistBoard(String name) {
        Board board = Board.of(name, BoardType.GENERAL);
        em.persist(board);
        return board;
    }

    private void persistPost(Board board, Member writer, String title, String content) {
        BoardCategory category = BoardCategory.of(board, "잡담", "chat-" + System.nanoTime());
        em.persist(category);
        em.persist(Post.of(title, content, false, false, false, board, category, writer));
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
