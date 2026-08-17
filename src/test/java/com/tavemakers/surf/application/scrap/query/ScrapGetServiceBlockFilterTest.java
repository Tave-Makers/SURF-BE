package com.tavemakers.surf.application.scrap.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.post.query.PostLikeGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 차단 관계로 스크랩 목록의 단방향 필터를 검증한다 */
@DataJpaTest
@Import({
        ScrapGetService.class,
        PostLikeGetService.class,
        BlockGetService.class,
})
class ScrapGetServiceBlockFilterTest {

    @Autowired
    private ScrapGetService scrapGetService;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager em;

    private Board board;
    private BoardCategory category;
    private Member viewer;
    private Member author;

    @BeforeEach
    void setUp() {
        board = Board.of("자유게시판", BoardType.GENERAL);
        em.persist(board);
        category = BoardCategory.of(board, "잡담", "chat-" + System.nanoTime());
        em.persist(category);

        viewer = persistMember("viewer");
        author = persistMember("author");
    }

    @Test
    @DisplayName("차단 이전에 스크랩해 둔 상대 게시글도 목록에서 사라진다")
    void 차단_이전_스크랩분도_사라진다() {
        // 스크랩이 차단보다 먼저다 — 이 순서가 요구사항의 이유다
        scrap(persistPost("상대글"));
        scrap(persistPost("내글", viewer));
        em.flush();

        blockRepository.save(Block.of(viewer.getId(), author.getId()));
        em.flush();
        em.clear();

        Slice<PostResDTO> result = scrapGetService.getMyScraps(viewer.getId(), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(PostResDTO::title).containsExactly("내글");
    }

    @Test
    @DisplayName("숨기기만 하고 scrap 레코드는 지우지 않는다 — 차단을 해제하면 목록에 그대로 돌아온다")
    void 차단_해제하면_스크랩이_복구된다() {
        Post post = persistPost("상대글");
        scrap(post);
        em.flush();

        Block block = blockRepository.save(Block.of(viewer.getId(), author.getId()));
        em.flush();
        em.clear();

        assertThat(scrapGetService.getMyScraps(viewer.getId(), PageRequest.of(0, 20)).getContent())
                .as("차단 중에는 보이지 않는다").isEmpty();

        assertThat(countScraps())
                .as("숨김이지 삭제가 아니다 — 레코드가 사라지면 복구할 수 없다").isEqualTo(1);

        blockRepository.delete(block);
        em.flush();
        em.clear();

        assertThat(scrapGetService.getMyScraps(viewer.getId(), PageRequest.of(0, 20)).getContent())
                .extracting(PostResDTO::title)
                .as("해제하면 그대로 돌아와야 한다").containsExactly("상대글");
    }

    @Test
    @DisplayName("차단은 단방향이다 — 상대가 나를 차단했을 뿐이면 내 스크랩은 그대로 보인다")
    void 상대가_나를_차단해도_내_스크랩은_그대로다() {
        scrap(persistPost("상대글"));
        em.flush();

        // 반대 방향: author → viewer
        blockRepository.save(Block.of(author.getId(), viewer.getId()));
        em.flush();
        em.clear();

        Slice<PostResDTO> result = scrapGetService.getMyScraps(viewer.getId(), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(PostResDTO::title)
                .as("양방향(existsBetween)으로 구현하면 여기서 사라진다")
                .containsExactly("상대글");
    }

    @Test
    @DisplayName("차단이 없으면 스크랩 목록이 그대로 조회된다")
    void 차단이_없으면_그대로_조회된다() {
        scrap(persistPost("상대글"));
        scrap(persistPost("내글", viewer));
        em.flush();
        em.clear();

        Slice<PostResDTO> result = scrapGetService.getMyScraps(viewer.getId(), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(PostResDTO::title)
                .containsExactlyInAnyOrder("상대글", "내글");
    }

    private long countScraps() {
        return em.createQuery(
                        "select count(s) from Scrap s where s.member.id = :id", Long.class)
                .setParameter("id", viewer.getId())
                .getSingleResult();
    }

    private Post persistPost(String title) {
        return persistPost(title, author);
    }

    private Post persistPost(String title, Member writer) {
        Post post = Post.of(title, "본문", false, false, false, board, category, writer);
        em.persist(post);
        return post;
    }

    private void scrap(Post post) {
        em.persist(Scrap.of(viewer, post));
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
