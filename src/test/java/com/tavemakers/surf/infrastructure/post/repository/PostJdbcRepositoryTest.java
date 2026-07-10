package com.tavemakers.surf.infrastructure.post.repository;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import com.tavemakers.surf.domain.post.entity.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조회수 벌크 업데이트 델타 합산 회귀 테스트.
 *
 * 과거에는 SET view_count = ?(절대값 덮어쓰기)여서 Redis 장애 폴백으로 DB에 직접 증가된
 * 조회수가 스케줄러 동기화 시 덮어써져 유실됐다. 현재는 SET view_count = view_count + ?
 * (델타 합산)이므로 DB의 기존 값 위에 Redis 델타가 더해져야 한다.
 */
@DataJpaTest
@Import(PostJdbcRepository.class)
class PostJdbcRepositoryTest {

    @Autowired
    private PostJdbcRepository postJdbcRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long postId;

    @BeforeEach
    void setUp() {
        Board board = Board.builder().name("자유게시판").type(BoardType.GENERAL).build();
        entityManager.persist(board);

        BoardCategory category = BoardCategory.builder()
                .board(board).name("잡담").slug("chat").build();
        entityManager.persist(category);

        Member author = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
                .name("회원")
                .email("author" + System.nanoTime() + "@test.com")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(author);

        Post post = Post.builder()
                .title("제목").content("내용")
                .board(board).boardName(board.getName())
                .category(category).categoryName(category.getName())
                .member(author)
                .viewCount(5)
                .build();
        entityManager.persist(post);
        entityManager.flush();

        this.postId = post.getId();
    }

    @Test
    @DisplayName("델타 3을 반영하면 기존 view_count 5에 합산되어 8이 된다 (덮어쓰기 금지)")
    void 델타는_기존_조회수에_합산된다() {
        postJdbcRepository.viewCountBulkUpdate(List.of(new PostViewUpdateDto(postId, 3)));

        Integer viewCount = jdbcTemplate.queryForObject(
                "SELECT view_count FROM post WHERE post_id = ?", Integer.class, postId);

        assertThat(viewCount)
                .as("델타가 절대값으로 덮어쓰지 않고 기존 값에 합산되어야 한다")
                .isEqualTo(8);
    }
}
