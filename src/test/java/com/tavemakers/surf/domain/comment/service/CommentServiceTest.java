package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.comment.event.CommentReplyEvent;
import com.tavemakers.surf.domain.comment.exception.CommentNotFoundException;
import com.tavemakers.surf.domain.comment.exception.DuplicateCommentException;
import com.tavemakers.surf.domain.comment.exception.InvalidBlankCommentException;
import com.tavemakers.surf.domain.comment.exception.InvalidReplyException;
import com.tavemakers.surf.domain.comment.exception.NotMyCommentException;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.post.service.support.PostCommentCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * CommentService 단위 테스트 — 루트/대댓글 분기, parentId 처리, 검증 예외, 삭제 로직을 겨냥한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostGetService postGetService;
    @Mock
    private MemberGetService memberGetService;
    @Mock
    private CommentMentionService commentMentionService;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private PostCommentCountService postCommentCountService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                postGetService,
                memberGetService,
                commentMentionService,
                commentLikeRepository,
                postCommentCountService,
                eventPublisher
        );
    }

    // ---------- 테스트 픽스처 ----------

    private Board board() {
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        ReflectionTestUtils.setField(board, "id", 1L);
        return board;
    }

    private BoardCategory category(Board board) {
        BoardCategory category = BoardCategory.of(board, "잡담", "chat");
        ReflectionTestUtils.setField(category, "id", 1L);
        return category;
    }

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

    private Post post(long id, Member owner, Board board, BoardCategory category) {
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .board(board)
                .boardName(board.getName())
                .category(category)
                .categoryName(category.getName())
                .member(owner)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    /** commentRepository.save 가 마치 영속화된 것처럼 id 를 채워 반환하도록 스텁한다. */
    private void stubSaveAssignsId(long id) {
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", id);
            return comment;
        });
    }

    // ---------- createComment: 직전 중복 요청 방지 ----------

    @Test
    @DisplayName("같은 작성자가 같은 게시글·부모에 같은 내용을 시간창 안에 다시 보내면 DuplicateCommentException — 저장·카운트 증가 없음")
    void createComment_duplicateWithinWindow_throwsAndSkipsSave() {
        Member author = member(2L, "작성자");
        Post post = post(10L, member(1L, "글쓴이"), board(), category(board()));
        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(author);
        given(commentRepository.existsRecentDuplicate(
                any(), any(), any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> commentService.createComment(10L, 2L, null, "중복 내용", List.of()))
                .isInstanceOf(DuplicateCommentException.class);

        then(commentRepository).should(never()).save(any());
        then(postCommentCountService).should(never()).increase(any());
    }

    @Test
    @DisplayName("중복이 아니면(시간창 밖/다른 내용) 정상 저장된다")
    void createComment_noDuplicate_savesNormally() {
        Member author = member(2L, "작성자");
        Post post = post(10L, member(1L, "글쓴이"), board(), category(board()));
        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(author);
        given(commentRepository.existsRecentDuplicate(
                any(), any(), any(), any(), any())).willReturn(false);
        stubSaveAssignsId(100L);

        Comment saved = commentService.createComment(10L, 2L, null, "새 내용", List.of());

        assertThat(saved.getId()).isEqualTo(100L);
        then(postCommentCountService).should().increase(10L);
    }

    // ---------- createComment: 루트 댓글 ----------

    @Test
    @DisplayName("루트 댓글 작성자가 게시글 작성자 본인이면 markAsRoot로 rootId가 세팅되고 알림 이벤트는 발행하지 않는다")
    void createComment_root_authorIsPostOwner_marksRootAndSkipsEvent() {
        Board board = board();
        BoardCategory category = category(board);
        Member owner = member(1L, "글쓴이");
        Post post = post(10L, owner, board, category);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(1L)).willReturn(owner);
        stubSaveAssignsId(100L);

        Comment saved = commentService.createComment(10L, 1L, null, "좋은 글이네요", null);

        assertThat(saved.getDepth()).isZero();
        assertThat(saved.getRootId()).isEqualTo(100L);
        then(eventPublisher).should(never()).publishEvent(any());
        then(commentMentionService).should().createMentions(saved, null);
        then(postCommentCountService).should().increase(10L);
    }

    @Test
    @DisplayName("루트 댓글 작성자가 게시글 작성자 본인이 아니면 CommentCreatedEvent를 발행한다")
    void createComment_root_authorNotPostOwner_publishesCommentCreatedEvent() {
        Board board = board();
        BoardCategory category = category(board);
        Member owner = member(1L, "글쓴이");
        Member commenter = member(2L, "댓글러");
        Post post = post(10L, owner, board, category);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(commenter);
        stubSaveAssignsId(100L);

        commentService.createComment(10L, 2L, null, "좋은 글이네요", null);

        ArgumentCaptor<CommentCreatedEvent> captor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        CommentCreatedEvent event = captor.getValue();
        assertThat(event.getReceiverId()).isEqualTo(1L);
        assertThat(event.getActorId()).isEqualTo(2L);
        assertThat(event.getActorName()).isEqualTo("댓글러");
        assertThat(event.getBoardId()).isEqualTo(board.getId());
        assertThat(event.getPostId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("빈 문자열 내용이면 InvalidBlankCommentException을 던지고 저장하지 않는다")
    void createComment_blankContent_throwsAndSkipsSave() {
        Board board = board();
        BoardCategory category = category(board);
        Member owner = member(1L, "글쓴이");
        Post post = post(10L, owner, board, category);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(1L)).willReturn(owner);

        assertThatThrownBy(() -> commentService.createComment(10L, 1L, null, "", null))
                .isInstanceOf(InvalidBlankCommentException.class);

        then(commentRepository).should(never()).save(any());
    }

    // ---------- createComment: 대댓글 ----------

    @Test
    @DisplayName("대댓글은 부모의 rootId·depth+1을 상속하고, 부모 작성자 자동 멘션 포함 시 CommentReplyEvent를 발행한다")
    void createComment_reply_inheritsParentRootAndDepth_publishesReplyEvent() {
        Board board = board();
        BoardCategory category = category(board);
        Member parentWriter = member(1L, "부모작성자");
        Member replier = member(2L, "대댓글러");
        Post post = post(10L, parentWriter, board, category);

        Comment parent = Comment.root(post, parentWriter, "부모 댓글");
        ReflectionTestUtils.setField(parent, "id", 50L);
        ReflectionTestUtils.setField(parent, "rootId", 50L);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(replier);
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));
        stubSaveAssignsId(200L);

        Comment saved = commentService.createComment(
                10L, 2L, 50L, "대댓글입니다", List.of(1L));

        assertThat(saved.getDepth()).isEqualTo(1);
        assertThat(saved.getRootId()).isEqualTo(50L);
        assertThat(saved.getParent()).isEqualTo(parent);

        ArgumentCaptor<CommentReplyEvent> captor = ArgumentCaptor.forClass(CommentReplyEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().getReceiverId()).isEqualTo(1L);
        assertThat(captor.getValue().getActorId()).isEqualTo(2L);

        then(commentMentionService).should().createMentions(saved, List.of(1L));
        then(postCommentCountService).should().increase(10L);
    }

    @Test
    @DisplayName("부모 댓글이 존재하지 않으면 CommentNotFoundException을 던진다")
    void createComment_reply_parentNotFound_throwsCommentNotFoundException() {
        given(postGetService.getPost(10L)).willReturn(post(10L, member(1L, "글쓴이"), board(), category(board())));
        given(memberGetService.getMember(2L)).willReturn(member(2L, "대댓글러"));
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.createComment(10L, 2L, 999L, "대댓글", List.of(1L)))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("부모 댓글이 다른 게시글 소속이면 CommentNotFoundException을 던진다")
    void createComment_reply_parentBelongsToDifferentPost_throwsCommentNotFoundException() {
        Board board = board();
        BoardCategory category = category(board);
        Member parentWriter = member(1L, "부모작성자");
        Post otherPost = post(999L, parentWriter, board, category);
        Post post = post(10L, parentWriter, board, category);

        Comment parent = Comment.root(otherPost, parentWriter, "다른 글 댓글");
        ReflectionTestUtils.setField(parent, "id", 50L);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(member(2L, "대댓글러"));
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));

        assertThatThrownBy(() ->
                commentService.createComment(10L, 2L, 50L, "대댓글", List.of(1L)))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("대댓글에 부모 작성자 멘션이 빠지면 InvalidReplyException을 던지고 저장하지 않는다")
    void createComment_reply_missingRequiredMention_throwsInvalidReplyException() {
        Board board = board();
        BoardCategory category = category(board);
        Member parentWriter = member(1L, "부모작성자");
        Post post = post(10L, parentWriter, board, category);

        Comment parent = Comment.root(post, parentWriter, "부모 댓글");
        ReflectionTestUtils.setField(parent, "id", 50L);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(2L)).willReturn(member(2L, "대댓글러"));
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));

        assertThatThrownBy(() ->
                commentService.createComment(10L, 2L, 50L, "대댓글", List.of(999L)))
                .isInstanceOf(InvalidReplyException.class);

        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("본인 댓글에 대댓글을 달 때는 멘션 검증을 생략하고 알림도 발행하지 않는다")
    void createComment_reply_selfReply_skipsMentionValidationAndEvent() {
        Board board = board();
        BoardCategory category = category(board);
        Member writer = member(1L, "작성자");
        Post post = post(10L, writer, board, category);

        Comment parent = Comment.root(post, writer, "부모 댓글");
        ReflectionTestUtils.setField(parent, "id", 50L);
        ReflectionTestUtils.setField(parent, "rootId", 50L);

        given(postGetService.getPost(10L)).willReturn(post);
        given(memberGetService.getMember(1L)).willReturn(writer);
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));
        stubSaveAssignsId(200L);

        Comment saved = commentService.createComment(10L, 1L, 50L, "셀프 대댓글", null);

        assertThat(saved.getDepth()).isEqualTo(1);
        then(eventPublisher).should(never()).publishEvent(any());
    }

    // ---------- deleteComment ----------

    @Test
    @DisplayName("존재하지 않는 댓글이면 CommentNotFoundException을 던진다")
    void deleteComment_notFound_throwsCommentNotFoundException() {
        given(commentRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(10L, 1L, 1L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("댓글이 다른 게시글 소속이면 NotMyCommentException을 던진다")
    void deleteComment_differentPost_throwsNotMyCommentException() {
        Board board = board();
        BoardCategory category = category(board);
        Member writer = member(1L, "작성자");
        Post post = post(999L, writer, board, category);
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(10L, 1L, 1L))
                .isInstanceOf(NotMyCommentException.class);
    }

    @Test
    @DisplayName("본인이 작성한 댓글이 아니면 NotMyCommentException을 던진다")
    void deleteComment_notAuthor_throwsNotMyCommentException() {
        Board board = board();
        BoardCategory category = category(board);
        Member writer = member(1L, "작성자");
        Post post = post(10L, writer, board, category);
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(member(2L, "다른회원"));

        assertThatThrownBy(() -> commentService.deleteComment(10L, 1L, 2L))
                .isInstanceOf(NotMyCommentException.class);
    }

    @Test
    @DisplayName("Manager는 본인 댓글이 아니어도 댓글을 삭제할 수 있다")
    void deleteComment_managerCanDeleteOthersComment() {
        Board board = board();
        BoardCategory category = category(board);
        Member writer = member(1L, "작성자");
        Member manager = Member.builder()
                .name("매니저")
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MANAGER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(manager, "id", 2L);

        Post post = post(10L, writer, board, category);
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(manager);

        commentService.deleteComment(10L, 1L, 2L);

        then(commentRepository).should().detachChildren(1L);
        then(commentLikeRepository).should().deleteAllByComment(comment);
        then(commentMentionService).should().deleteAllByComment(comment);
        then(commentRepository).should().delete(comment);
        then(postCommentCountService).should().decrease(10L);
    }

    @Test
    @DisplayName("정상 삭제 시 자식 댓글 detach → 연관 데이터 삭제 → 댓글 삭제 → 댓글수 감소 순으로 처리한다")
    void deleteComment_valid_detachesChildrenThenDeletesAssociatedDataAndDecreasesCount() {
        Board board = board();
        BoardCategory category = category(board);
        Member writer = member(1L, "작성자");
        Post post = post(10L, writer, board, category);
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(1L)).willReturn(writer);

        commentService.deleteComment(10L, 1L, 1L);

        InOrder order = inOrder(
                commentRepository, commentLikeRepository, commentMentionService, postCommentCountService);
        order.verify(commentRepository).detachChildren(1L);
        order.verify(commentLikeRepository).deleteAllByComment(comment);
        order.verify(commentMentionService).deleteAllByComment(comment);
        order.verify(commentRepository).delete(comment);
        order.verify(postCommentCountService).decrease(10L);
    }
}
