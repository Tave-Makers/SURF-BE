package com.tavemakers.surf.application.comment.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 댓글 목록의 차단 필터 배선 (이슈 #370).
 *
 * <p>Slice 와 totalCount 에 <b>같은 제외 집합</b>이 가는지 고정한다. 하나만 필터링하면
 * "댓글 3개"라고 표시되는데 목록은 비어 보이는 화면이 나온다.
 */
@ExtendWith(MockitoExtension.class)
class CommentGetServiceBlockFilterTest {

    private static final Long POST_ID = 100L;
    private static final Long VIEWER = 1L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMentionGetService commentMentionGetService;
    @Mock
    private CommentLikeService commentLikeService;
    @Mock
    private BlockGetService blockGetService;

    @InjectMocks
    private CommentGetService commentGetService;

    @Test
    @DisplayName("Slice 와 totalCount 에 같은 제외 집합을 넘긴다")
    void 목록과_카운트에_같은_집합을_넘긴다() {
        Set<Long> excluded = Set.of(2L, 3L);
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(excluded);
        given(commentRepository.findByPostIdExcludingAuthors(POST_ID, excluded, PAGEABLE))
                .willReturn(new SliceImpl<>(List.<Comment>of(), PAGEABLE, false));
        given(commentRepository.countByPostIdExcludingAuthors(POST_ID, excluded)).willReturn(0L);
        given(commentMentionGetService.getMentionsByCommentIds(anyList())).willReturn(Map.of());

        CommentListResDTO result = commentGetService.getComments(POST_ID, PAGEABLE, VIEWER);

        then(commentRepository).should().findByPostIdExcludingAuthors(POST_ID, excluded, PAGEABLE);
        then(commentRepository).should().countByPostIdExcludingAuthors(POST_ID, excluded);
        assertThat(result.totalCount()).isZero();

        // 필터 없는 기존 쿼리로 회귀하면 차단한 사람의 댓글이 그대로 보인다
        then(commentRepository).should(never()).findByPostIdOrderByCreatedAtAsc(anyLong(), any());
        then(commentRepository).should(never()).countByPostId(anyLong());
    }
}
