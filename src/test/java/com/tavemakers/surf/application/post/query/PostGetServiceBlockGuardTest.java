package com.tavemakers.surf.application.post.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.reservation.query.ReservationGetService;
import com.tavemakers.surf.application.scrap.query.ScrapGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.support.ViewCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 게시글 상세의 차단 가드 (이슈 #370).
 *
 * <p>가드가 조회수 증가·스크랩·좋아요·첨부 조회보다 <b>앞</b>에 있어야 한다. 뒤로 밀리면 숨겨야 할 글의
 * 조회수가 오르고 부수효과가 남는다 — 응답만 404면 겉으로는 정상으로 보여 놓치기 쉬운 회귀다.
 *
 * <p>403이 아니라 404인 이유는 차단 사실을 상대에게 노출하지 않기 위해서다.
 */
@ExtendWith(MockitoExtension.class)
class PostGetServiceBlockGuardTest {

    private static final Long VIEWER = 1L;
    private static final Long AUTHOR = 2L;
    private static final Long POST_ID = 100L;

    @Mock
    private PostRepository postRepository;
    @Mock
    private BlockGetService blockGetService;
    @Mock
    private ScrapGetService scrapGetService;
    @Mock
    private PostLikeService postLikeService;
    @Mock
    private PostImageGetService imageGetService;
    @Mock
    private PostFileGetService fileGetService;
    @Mock
    private ViewCountService viewCountService;
    @Mock
    private ReservationGetService reservationGetService;

    @InjectMocks
    private PostGetService postGetService;

    private Post post;

    @BeforeEach
    void setUp() {
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        BoardCategory category = BoardCategory.of(board, "잡담", "chat");
        Member author = Member.builder().name("작성자").build();
        ReflectionTestUtils.setField(author, "id", AUTHOR);
        post = Post.of("제목", "본문", false, false, false, board, category, author);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    @DisplayName("차단한 작성자의 게시글 상세는 404다 — 403이면 차단 사실이 드러난다")
    void 차단한_작성자의_상세는_404다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(blockGetService.isBlockedByMe(VIEWER, AUTHOR)).willReturn(true);

        assertThatThrownBy(() -> postGetService.getPostDetail(POST_ID, VIEWER))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("차단으로 막힌 상세는 조회수를 올리지 않고 스크랩·좋아요도 조회하지 않는다")
    void 차단시_부수효과가_없다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(blockGetService.isBlockedByMe(VIEWER, AUTHOR)).willReturn(true);

        assertThatThrownBy(() -> postGetService.getPostDetail(POST_ID, VIEWER))
                .isInstanceOf(PostNotFoundException.class);

        then(viewCountService).should(never()).increaseViewCount(any(), anyLong());
        then(scrapGetService).should(never()).isScrappedByMe(anyLong(), anyLong());
        then(postLikeService).should(never()).isLikedByMe(anyLong(), anyLong());
    }

    @Test
    @DisplayName("차단하지 않은 작성자의 게시글은 그대로 조회된다")
    void 차단하지_않으면_정상_조회된다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(blockGetService.isBlockedByMe(VIEWER, AUTHOR)).willReturn(false);

        assertThatCode(() -> postGetService.getPostDetail(POST_ID, VIEWER)).doesNotThrowAnyException();

        then(viewCountService).should().increaseViewCount(post, VIEWER);
    }

    @Test
    @DisplayName("validateVisiblePost — 차단한 작성자의 글이면 404 (댓글 목록의 선행 가드)")
    void 가드는_차단시_404다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(blockGetService.isBlockedByMe(VIEWER, AUTHOR)).willReturn(true);

        assertThatThrownBy(() -> postGetService.validateVisiblePost(POST_ID, VIEWER))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("validateVisiblePost — 없는 게시글도 동일하게 404")
    void 가드는_없는_게시글도_404다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postGetService.validateVisiblePost(POST_ID, VIEWER))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("내부 조합용 조회는 차단 필터를 타지 않는다 — scheduler·event 가 깨지면 안 된다")
    void 내부_조회는_필터링하지_않는다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        assertThatCode(() -> postGetService.readPost(POST_ID)).doesNotThrowAnyException();

        then(blockGetService).should(never()).isBlockedByMe(anyLong(), anyLong());
    }
}
