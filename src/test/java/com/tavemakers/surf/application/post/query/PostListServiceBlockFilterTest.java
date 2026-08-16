package com.tavemakers.surf.application.post.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.board.query.BoardCategoryGetService;
import com.tavemakers.surf.application.board.query.BoardGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 게시글 목록의 차단 필터 배선 (이슈 #370).
 *
 * <p>제외 집합이 실제로 쿼리에 전달되는지, 그리고 특정 작성자 목록이 <b>조회 자체를 생략</b>하는지 본다.
 * 후자는 DB 페이지를 읽고 버리는 방식이면 페이지네이션이 깨지므로 호출 부재까지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PostListServiceBlockFilterTest {

    private static final Long VIEWER = 1L;
    private static final Long BLOCKED_AUTHOR = 2L;
    private static final Long BOARD_ID = 10L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private PostRepository postRepository;
    @Mock
    private MemberGetService memberGetService;
    @Mock
    private BoardGetService boardGetService;
    @Mock
    private BoardCategoryGetService boardCategoryGetService;
    @Mock
    private BlockGetService blockGetService;
    @Mock
    private FlagsMapper flagsMapper;

    @InjectMocks
    private PostListService postListService;

    @Test
    @DisplayName("차단한 작성자의 글 목록은 조회 없이 빈 Slice 다 — 읽고 버리면 페이지네이션이 깨진다")
    void 차단한_작성자_목록은_조회를_생략한다() {
        given(blockGetService.isBlockedByMe(VIEWER, BLOCKED_AUTHOR)).willReturn(true);

        Slice<PostResDTO> result = postListService.getPostsByMember(BLOCKED_AUTHOR, VIEWER, PAGEABLE);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        then(postRepository).should(never()).findByMemberId(anyLong(), any());
    }

    @Test
    @DisplayName("차단하지 않은 작성자의 글 목록은 정상 조회된다")
    void 차단하지_않은_작성자는_정상_조회된다() {
        given(blockGetService.isBlockedByMe(VIEWER, BLOCKED_AUTHOR)).willReturn(false);
        given(postRepository.findByMemberId(BLOCKED_AUTHOR, PAGEABLE))
                .willReturn(new SliceImpl<>(List.of(), PAGEABLE, false));

        postListService.getPostsByMember(BLOCKED_AUTHOR, VIEWER, PAGEABLE);

        then(postRepository).should().findByMemberId(BLOCKED_AUTHOR, PAGEABLE);
    }

    @Test
    @DisplayName("게시판 목록(slug)은 제외 집합을 쿼리에 그대로 넘긴다")
    void 게시판_목록은_제외_집합을_전달한다() {
        Set<Long> excluded = Set.of(BLOCKED_AUTHOR);
        given(boardGetService.getBoard(BOARD_ID)).willReturn(Board.of("자유게시판", BoardType.GENERAL));
        given(memberGetService.getMember(VIEWER)).willReturn(normalViewer());
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(excluded);
        given(postRepository.findByBoardIdAndIsReservedFalseExcludingAuthors(BOARD_ID, excluded, PAGEABLE))
                .willReturn(new SliceImpl<>(List.of(), PAGEABLE, false));

        postListService.getPostsByBoardAndCategory(BOARD_ID, "all", VIEWER, PAGEABLE);

        then(postRepository).should()
                .findByBoardIdAndIsReservedFalseExcludingAuthors(BOARD_ID, excluded, PAGEABLE);
        // 필터 없는 기존 쿼리로 회귀하면 차단 글이 그대로 노출된다
        then(postRepository).should(never()).findByBoardIdAndIsReservedFalse(anyLong(), any());
    }

    @Test
    @DisplayName("관리자 뷰(예약글 포함)도 차단 필터를 적용한다")
    void 관리자_뷰도_필터를_적용한다() {
        Set<Long> excluded = Set.of(BLOCKED_AUTHOR);
        given(boardGetService.getBoard(BOARD_ID)).willReturn(Board.of("자유게시판", BoardType.GENERAL));
        given(memberGetService.getMember(VIEWER)).willReturn(managerViewer());
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(excluded);
        given(postRepository.findByBoardIdExcludingAuthors(BOARD_ID, excluded, PAGEABLE))
                .willReturn(new SliceImpl<>(List.of(), PAGEABLE, false));

        postListService.getPostsByBoardAndCategory(BOARD_ID, "all", VIEWER, PAGEABLE);

        then(postRepository).should().findByBoardIdExcludingAuthors(BOARD_ID, excluded, PAGEABLE);
        then(postRepository).should(never()).findByBoardId(anyLong(), any());
    }

    @Test
    @DisplayName("내 글 목록은 차단 필터 대상이 아니다 — 자기 자신은 차단할 수 없다")
    void 내_글_목록은_필터링하지_않는다() {
        given(postRepository.findByMemberId(VIEWER, PAGEABLE))
                .willReturn(new SliceImpl<>(List.<Post>of(), PAGEABLE, false));

        postListService.getMyPosts(VIEWER, PAGEABLE);

        then(blockGetService).should(never()).getMyBlockedMemberIds(anyLong());
        then(blockGetService).should(never()).isBlockedByMe(anyLong(), anyLong());
    }

    private Member normalViewer() {
        Member viewer = Member.builder().name("뷰어").role(MemberRole.MEMBER).build();
        ReflectionTestUtils.setField(viewer, "id", VIEWER);
        return viewer;
    }

    private Member managerViewer() {
        Member viewer = Member.builder().name("매니저").role(MemberRole.MANAGER).build();
        ReflectionTestUtils.setField(viewer, "id", VIEWER);
        return viewer;
    }
}
