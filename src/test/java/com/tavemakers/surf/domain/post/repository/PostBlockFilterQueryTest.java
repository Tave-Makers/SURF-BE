package com.tavemakers.surf.domain.post.repository;

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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 차단 작성자 제외 JPQL 실검증 (이슈 #370).
 *
 * <p>필터를 DTO 생성 후 stream 으로 걸러내면 "페이지 20건을 읽고 3건을 버려 17건만 내려주는" 형태가 되어
 * 페이지 크기가 들쭉날쭉해지고 무한스크롤이 조기 종료된다. 그래서 쿼리에서 걸러내며, 이 테스트는
 * <b>제외가 실제로 SQL에서 일어나고 페이지가 요청 크기만큼 채워지는지</b>를 본다.
 *
 * <p>제외 집합은 절대 비어 있지 않다는 계약({@code BlockGetService.getMyBlockedMemberIds})에 기대므로,
 * 차단 0건일 때 쓰는 sentinel 경로도 함께 고정한다.
 */
@DataJpaTest
class PostBlockFilterQueryTest {

    /** 차단이 0건일 때 BlockGetService 가 넣어주는 값 — 어떤 회원과도 일치하지 않는다 */
    private static final Set<Long> NO_ONE_BLOCKED = Set.of(-1L);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager em;

    private Board board;
    private BoardCategory category;
    private Member blockedAuthor;
    private Member normalAuthor;

    @BeforeEach
    void setUp() {
        board = persistBoard("자유게시판");
        category = persistCategory(board);
        blockedAuthor = persistMember("blocked");
        normalAuthor = persistMember("normal");
    }

    @Test
    @DisplayName("게시판 목록에서 차단 작성자의 글이 빠진다")
    void 게시판_목록에서_차단_작성자가_빠진다() {
        persistPost("차단글", blockedAuthor, false);
        persistPost("정상글", normalAuthor, false);
        em.flush();

        Slice<Post> result = postRepository.findByBoardIdExcludingAuthors(
                board.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("정상글");
    }

    @Test
    @DisplayName("차단이 없으면(sentinel) 아무도 제외되지 않는다 — 빈 컬렉션을 not in 에 넘기지 않기 위한 계약")
    void sentinel이면_아무도_제외되지_않는다() {
        persistPost("글1", blockedAuthor, false);
        persistPost("글2", normalAuthor, false);
        em.flush();

        Slice<Post> result = postRepository.findByBoardIdExcludingAuthors(
                board.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("차단 작성자를 제외해도 페이지는 요청한 크기만큼 채워진다 — 조회 후 필터링이 아니다")
    void 페이지_크기가_유지된다() {
        // 차단 작성자 글 5건이 앞쪽에 섞여 있어도, 정상 글 3건을 요청하면 3건이 와야 한다
        for (int i = 0; i < 5; i++) {
            persistPost("차단글" + i, blockedAuthor, false);
        }
        for (int i = 0; i < 4; i++) {
            persistPost("정상글" + i, normalAuthor, false);
        }
        em.flush();

        Slice<Post> result = postRepository.findByBoardIdExcludingAuthors(
                board.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 3));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()).extracting(p -> p.getMember().getId())
                .containsOnly(normalAuthor.getId());
    }

    @Test
    @DisplayName("예약글 제외 변형은 차단과 예약을 함께 걸러낸다")
    void 예약글_제외_변형도_함께_적용된다() {
        persistPost("차단-공개", blockedAuthor, false);
        persistPost("정상-예약", normalAuthor, true);
        persistPost("정상-공개", normalAuthor, false);
        em.flush();

        Slice<Post> result = postRepository.findByBoardIdAndIsReservedFalseExcludingAuthors(
                board.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("정상-공개");
    }

    @Test
    @DisplayName("카테고리 변형도 차단 작성자를 제외한다")
    void 카테고리_변형도_제외한다() {
        persistPost("차단글", blockedAuthor, false);
        persistPost("정상글", normalAuthor, false);
        em.flush();

        Slice<Post> withReserved = postRepository.findByBoardIdAndCategoryIdExcludingAuthors(
                board.getId(), category.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));
        Slice<Post> withoutReserved = postRepository.findByBoardIdAndCategoryIdAndIsReservedFalseExcludingAuthors(
                board.getId(), category.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));

        assertThat(withReserved.getContent()).extracting(Post::getTitle).containsExactly("정상글");
        assertThat(withoutReserved.getContent()).extracting(Post::getTitle).containsExactly("정상글");
    }

    @Test
    @DisplayName("통합 검색과 게시판 내 검색 모두 차단 작성자를 제외한다")
    void 검색도_차단_작성자를_제외한다() {
        persistPost("서핑 차단글", blockedAuthor, false);
        persistPost("서핑 정상글", normalAuthor, false);
        em.flush();

        Slice<Post> all = postRepository.searchExcludingAuthors(
                "서핑", Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));
        Slice<Post> inBoard = postRepository.searchInBoardExcludingAuthors(
                board.getId(), "서핑", Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));

        assertThat(all.getContent()).extracting(Post::getTitle).containsExactly("서핑 정상글");
        assertThat(inBoard.getContent()).extracting(Post::getTitle).containsExactly("서핑 정상글");
    }

    @Test
    @DisplayName("차단은 단방향이다 — 상대 시점에서는 내 글이 그대로 보인다")
    void 차단은_단방향이다() {
        persistPost("내글", normalAuthor, false);
        persistPost("상대글", blockedAuthor, false);
        em.flush();

        // 같은 차단 관계를 양쪽 시점에서 조회해 비대칭을 대비시킨다.
        // 차단자 시점: blockedAuthor 를 차단했으므로 제외 집합에 들어가 "상대글"이 빠진다
        Slice<Post> fromBlockerSide = postRepository.findByBoardIdExcludingAuthors(
                board.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));
        // 피차단자 시점: 자신은 차단한 적이 없어 제외 집합이 sentinel 이므로 둘 다 보인다
        Slice<Post> fromOtherSide = postRepository.findByBoardIdExcludingAuthors(
                board.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        assertThat(fromBlockerSide.getContent()).extracting(Post::getTitle)
                .containsExactly("내글");
        assertThat(fromOtherSide.getContent()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("내글", "상대글");
    }

    private Board persistBoard(String name) {
        Board board = Board.of(name, BoardType.GENERAL);
        em.persist(board);
        return board;
    }

    private BoardCategory persistCategory(Board board) {
        BoardCategory category = BoardCategory.of(board, "잡담", "chat-" + System.nanoTime());
        em.persist(category);
        return category;
    }

    private void persistPost(String title, Member writer, boolean isReserved) {
        em.persist(Post.of(title, "본문", false, isReserved, false, board, category, writer));
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
