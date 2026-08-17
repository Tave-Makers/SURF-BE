package com.tavemakers.surf.domain.scrap.repository;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 스크랩 목록의 작성자 제외·정렬·fetch join 쿼리를 검증한다 */
@DataJpaTest
class ScrapBlockFilterQueryTest {

    /** 차단이 0건일 때 BlockGetService 가 넣어주는 값 */
    private static final Set<Long> NO_ONE_BLOCKED = Set.of(-1L);

    @Autowired
    private ScrapRepository scrapRepository;

    @Autowired
    private EntityManager em;

    private Board board;
    private BoardCategory category;
    private Member viewer;
    private Member blockedAuthor;
    private Member normalAuthor;

    @BeforeEach
    void setUp() {
        board = Board.of("자유게시판", BoardType.GENERAL);
        em.persist(board);
        category = BoardCategory.of(board, "잡담", "chat-" + System.nanoTime());
        em.persist(category);

        viewer = persistMember("viewer");
        blockedAuthor = persistMember("blocked");
        normalAuthor = persistMember("normal");
    }

    @Test
    @DisplayName("차단 작성자의 글이 스크랩 목록에서 빠진다 — 스크랩은 차단보다 먼저 한 행위다")
    void 차단_작성자의_스크랩이_빠진다() {
        scrap(persistPost("차단글", blockedAuthor));
        scrap(persistPost("정상글", normalAuthor));
        em.flush();
        em.clear();

        Slice<Post> result = scrapRepository.findPostsByMemberIdExcludingAuthors(
                viewer.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("정상글");
    }

    @Test
    @DisplayName("차단이 없으면(sentinel) 아무도 제외되지 않는다")
    void sentinel이면_아무도_제외되지_않는다() {
        scrap(persistPost("글1", blockedAuthor));
        scrap(persistPost("글2", normalAuthor));
        em.flush();
        em.clear();

        Slice<Post> result = scrapRepository.findPostsByMemberIdExcludingAuthors(
                viewer.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("정렬은 게시글 작성순이 아니라 내가 스크랩한 순서(최신순)다")
    void 스크랩한_순서가_유지된다() {
        Post first = persistPost("먼저 스크랩", normalAuthor);
        Post second = persistPost("나중에 스크랩", normalAuthor);
        scrap(first);
        scrap(second);
        em.flush();

        // 같은 트랜잭션에서 연속 persist 하면 createdAt 이 동률로 찍힐 수 있어 순서 검증이 흔들린다.
        // 보조 정렬 컬럼이 없는 쿼리라 시각을 명시적으로 벌려 결정적으로 만든다.
        setScrapCreatedAt(first, LocalDateTime.of(2026, 1, 1, 0, 0));
        setScrapCreatedAt(second, LocalDateTime.of(2026, 6, 1, 0, 0));
        em.clear();

        Slice<Post> result = scrapRepository.findPostsByMemberIdExcludingAuthors(
                viewer.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Post::getTitle)
                .as("스크랩한 순서의 역순이어야 한다 — order by s.createdAt desc 가 빠지면 무너진다")
                .containsExactly("나중에 스크랩", "먼저 스크랩");
    }

    @Test
    @DisplayName("작성자·게시판·카테고리가 fetch join 으로 함께 로딩된다 — 목록 변환 시 N+1 방지")
    void 연관관계가_함께_로딩된다() {
        scrap(persistPost("정상글", normalAuthor));
        em.flush();
        em.clear();

        Slice<Post> result = scrapRepository.findPostsByMemberIdExcludingAuthors(
                viewer.getId(), NO_ONE_BLOCKED, PageRequest.of(0, 20));

        Post post = result.getContent().get(0);
        assertThat(Hibernate.isInitialized(post.getMember()))
                .as("PostResDTO 변환이 작성자를 읽으므로 초기화되어 있어야 한다").isTrue();
        assertThat(Hibernate.isInitialized(post.getBoard())).isTrue();
        assertThat(Hibernate.isInitialized(post.getCategory())).isTrue();
    }

    @Test
    @DisplayName("차단 작성자를 제외해도 페이지는 요청한 크기만큼 채워진다 — 조회 후 필터링이 아니다")
    void 페이지_크기가_유지된다() {
        for (int i = 0; i < 5; i++) {
            scrap(persistPost("차단글" + i, blockedAuthor));
        }
        for (int i = 0; i < 4; i++) {
            scrap(persistPost("정상글" + i, normalAuthor));
        }
        em.flush();
        em.clear();

        Slice<Post> result = scrapRepository.findPostsByMemberIdExcludingAuthors(
                viewer.getId(), Set.of(blockedAuthor.getId()), PageRequest.of(0, 3));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()).extracting(p -> p.getMember().getId())
                .containsOnly(normalAuthor.getId());
    }

    private Post persistPost(String title, Member author) {
        Post post = Post.of(title, "본문", false, false, false, board, category, author);
        em.persist(post);
        return post;
    }

    private void scrap(Post post) {
        em.persist(Scrap.of(viewer, post));
    }

    /** createdAt 은 감사(@CreatedDate)로 채워지고 updatable=false 라 네이티브로 직접 조정한다 */
    private void setScrapCreatedAt(Post post, LocalDateTime createdAt) {
        em.createNativeQuery("update scrap set created_at = :createdAt where post_id = :postId")
                .setParameter("createdAt", createdAt)
                .setParameter("postId", post.getId())
                .executeUpdate();
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
