package com.tavemakers.surf.application.comment.usecase;

import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.comment.query.CommentMentionGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.service.CommentService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.MentionResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * CommentUsecase는 도메인 서비스(CommentService/CommentGetService)를 mock 하여
 * "엔티티 → ResDTO 매핑"과 "위임 인자 전달·로깅 분기"만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentUsecaseTest {

    @Mock
    private CommentService commentService;
    @Mock
    private CommentGetService commentGetService;
    @Mock
    private CommentMentionGetService commentMentionGetService;
    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private CommentUsecase commentUsecase;

    private Member member(long id, String name) {
        Member member = Member.builder()
                .name(name)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Comment comment(long id, Member writer) {
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        ReflectionTestUtils.setField(board, "id", 1L);
        BoardCategory category = BoardCategory.of(board, "잡담", "chat");
        ReflectionTestUtils.setField(category, "id", 1L);
        Post post = Post.builder()
                .title("제목").content("내용")
                .board(board).boardName(board.getName())
                .category(category).categoryName(category.getName())
                .member(writer)
                .build();
        ReflectionTestUtils.setField(post, "id", 10L);
        Comment comment = Comment.root(post, writer, "좋은 글이네요");
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "rootId", id);
        return comment;
    }

    @Test
    @DisplayName("댓글 생성은 도메인 서비스에 위임하고, 저장된 엔티티와 멘션을 CommentResDTO로 매핑한다(신규 댓글은 liked=false)")
    void createComment_delegatesToServiceAndMapsToResDTO() {
        Member writer = member(2L, "댓글러");
        Comment saved = comment(100L, writer);
        CommentCreateReqDTO req = new CommentCreateReqDTO(null, "좋은 글이네요", List.of(5L));
        List<MentionResDTO> mentions = List.of(new MentionResDTO(5L, "멘션된회원"));

        given(commentService.createComment(10L, 2L, null, "좋은 글이네요", List.of(5L)))
                .willReturn(saved);
        given(commentMentionGetService.getMentions(100L)).willReturn(mentions);

        CommentResDTO result = commentUsecase.createComment(10L, 2L, req);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.rootId()).isEqualTo(100L);
        assertThat(result.parentId()).isNull();
        assertThat(result.content()).isEqualTo("좋은 글이네요");
        assertThat(result.memberId()).isEqualTo(2L);
        assertThat(result.nickname()).isEqualTo("댓글러");
        assertThat(result.liked()).isFalse();
        assertThat(result.mentions()).isEqualTo(mentions);

        then(logEventEmitter).should().emit("comment.create", Map.of("post_id", 10L, "comment_id", 100L));
    }

    @Test
    @DisplayName("댓글 삭제는 postId·commentId·memberId를 그대로 도메인 서비스에 위임한다")
    void deleteComment_delegatesToService() {
        commentUsecase.deleteComment(10L, 100L, 2L);

        then(commentService).should().deleteComment(10L, 100L, 2L);
    }

    @Test
    @DisplayName("첫 페이지(0) 조회 시에는 목록 더보기 로그를 남기지 않는다")
    void getComments_firstPage_doesNotLogExpandEvent() {
        Pageable pageable = PageRequest.of(0, 10);
        CommentListResDTO expected = new CommentListResDTO(List.of(), 0, false);
        given(commentGetService.getComments(10L, pageable, 2L)).willReturn(expected);

        CommentListResDTO result = commentUsecase.getComments(10L, pageable, 2L);

        assertThat(result).isEqualTo(expected);
        then(logEventEmitter).should(never()).emit(any(), any());
    }

    @Test
    @DisplayName("다음 페이지(>0) 조회 시에는 로드된 댓글 수와 함께 목록 더보기 로그를 남긴다")
    void getComments_nextPage_logsExpandEventWithLoadedCount() {
        Pageable pageable = PageRequest.of(1, 10);
        Member writer = member(1L, "작성자");
        CommentResDTO dto = CommentResDTO.from(comment(1L, writer), 10L, List.of(), false);
        CommentListResDTO expected = new CommentListResDTO(List.of(dto), 11, true);
        given(commentGetService.getComments(10L, pageable, 2L)).willReturn(expected);

        commentUsecase.getComments(10L, pageable, 2L);

        then(logEventEmitter).should().emit("comment.list.expand", Map.of("post_id", 10L, "loaded_count", 1));
    }
}
