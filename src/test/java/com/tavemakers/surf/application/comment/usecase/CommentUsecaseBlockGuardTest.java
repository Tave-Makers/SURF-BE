package com.tavemakers.surf.application.comment.usecase;

import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.comment.query.CommentMentionGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.comment.service.CommentService;
import com.tavemakers.surf.domain.post.exception.PostNotFoundException;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

/**
 * 댓글 목록의 게시글 가시성 선행 가드 (이슈 #370).
 *
 * <p>게시글 상세가 404로 막히는데 댓글 목록은 열려 있으면, 차단한 상대의 글 내용을 댓글 스레드로
 * 우회 열람하게 된다. 목록 조회 전에 가드를 통과해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentUsecaseBlockGuardTest {

    private static final Long POST_ID = 100L;
    private static final Long VIEWER = 1L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CommentService commentService;
    @Mock
    private CommentGetService commentGetService;
    @Mock
    private CommentMentionGetService commentMentionGetService;
    @Mock
    private PostGetService postGetService;
    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private CommentUsecase commentUsecase;

    @Test
    @DisplayName("게시글이 차단으로 가려지면 댓글 목록도 404이며 댓글 조회를 시도하지 않는다")
    void 가려진_게시글의_댓글은_조회되지_않는다() {
        willThrow(new PostNotFoundException())
                .given(postGetService).validateVisiblePost(POST_ID, VIEWER);

        assertThatThrownBy(() -> commentUsecase.getComments(POST_ID, PAGEABLE, VIEWER))
                .isInstanceOf(PostNotFoundException.class);

        then(commentGetService).should(never()).getComments(anyLong(), any(), anyLong());
    }
}
