package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentLike;
import com.tavemakers.surf.domain.comment.event.CommentLikedEvent;
import com.tavemakers.surf.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.tavemakers.surf.domain.comment.exception.CommentNotFoundException;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * CommentLikeService 단위 테스트 — 좋아요 토글(등록/취소) 분기, 동시 중복 등록 예외 변환,
 * 알림 발행 조건(자기 좋아요 제외, 작성자 없음)을 겨냥한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MemberGetService memberGetService;
    @Mock
    private PostGetService postGetService;
    @Mock
    private LogEventEmitter logEventEmitter;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommentLikeService commentLikeService;

    @BeforeEach
    void setUp() {
        commentLikeService = new CommentLikeService(
                commentLikeRepository, commentRepository, memberGetService, postGetService,
                logEventEmitter, eventPublisher);
    }

    private Member member(long id) {
        Member member = Member.builder()
                .name("회원" + id)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Post post(long id, Member owner) {
        Board board = Board.of("자유게시판", BoardType.GENERAL);
        ReflectionTestUtils.setField(board, "id", 1L);
        BoardCategory category = BoardCategory.of(board, "잡담", "chat");
        ReflectionTestUtils.setField(category, "id", 1L);
        Post post = Post.builder()
                .title("제목").content("내용")
                .board(board).boardName(board.getName())
                .category(category).categoryName(category.getName())
                .member(owner)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment comment(long id, Post post, Member writer) {
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    @DisplayName("이미 좋아요를 눌렀으면 좋아요를 삭제하고 카운트를 감소시키며 false를 반환한다")
    void toggleLike_alreadyLiked_removesAndDecreasesCount_returnsFalse() {
        Member writer = member(1L);
        Member liker = member(2L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(liker);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, liker)).willReturn(1);

        boolean liked = commentLikeService.toggleLike(5L, 2L);

        assertThat(liked).isFalse();
        then(commentRepository).should().decreaseLikeCount(5L);
        then(commentLikeRepository).should(never()).saveAndFlush(any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("좋아요가 없었으면 새로 등록하고 카운트를 증가시키며 true를 반환한다")
    void toggleLike_notLiked_addsAndIncreasesCount_returnsTrue() {
        Member writer = member(1L);
        Member liker = member(2L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(liker);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, liker)).willReturn(0);
        given(commentRepository.findCommentOwnerId(5L)).willReturn(1L);

        boolean liked = commentLikeService.toggleLike(5L, 2L);

        assertThat(liked).isTrue();
        then(commentLikeRepository).should().saveAndFlush(any(CommentLike.class));
        then(commentRepository).should().increaseLikeCount(5L);
    }

    @Test
    @DisplayName("저장 시점에 unique 제약 위반이 발생하면 CommentLikeAlreadyExistsException으로 변환한다")
    void toggleLike_saveFlushViolatesUniqueConstraint_throwsAlreadyExistsException() {
        Member writer = member(1L);
        Member liker = member(2L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(liker);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, liker)).willReturn(0);
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> commentLikeService.toggleLike(5L, 2L))
                .isInstanceOf(CommentLikeAlreadyExistsException.class);

        then(commentRepository).should(never()).increaseLikeCount(5L);
    }

    @Test
    @DisplayName("자기 댓글에 좋아요를 누르면 알림을 발행하지 않는다")
    void toggleLike_selfLike_doesNotPublishNotification() {
        Member writer = member(1L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(1L)).willReturn(writer);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, writer)).willReturn(0);
        given(commentRepository.findCommentOwnerId(5L)).willReturn(1L);

        commentLikeService.toggleLike(5L, 1L);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("다른 사람이 좋아요를 누르면 댓글 작성자에게 CommentLikedEvent를 발행한다")
    void toggleLike_othersLike_publishesCommentLikedEvent() {
        Member writer = member(1L);
        Member liker = member(2L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(liker);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, liker)).willReturn(0);
        given(commentRepository.findCommentOwnerId(5L)).willReturn(1L);

        commentLikeService.toggleLike(5L, 2L);

        ArgumentCaptor<CommentLikedEvent> captor = ArgumentCaptor.forClass(CommentLikedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().getReceiverId()).isEqualTo(1L);
        assertThat(captor.getValue().getActorId()).isEqualTo(2L);
        assertThat(captor.getValue().getPostId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("댓글 작성자를 찾을 수 없으면(탈퇴 등) 알림을 발행하지 않는다")
    void toggleLike_commentOwnerIdNull_doesNotPublishNotification() {
        Member writer = member(1L);
        Member liker = member(2L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        given(commentRepository.findById(5L)).willReturn(Optional.of(comment));
        given(memberGetService.getMember(2L)).willReturn(liker);
        given(postGetService.getPost(10L)).willReturn(post);
        given(commentLikeRepository.deleteByCommentAndMember(comment, liker)).willReturn(0);
        given(commentRepository.findCommentOwnerId(5L)).willReturn(null);

        commentLikeService.toggleLike(5L, 2L);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글의 좋아요 수를 조회하면 CommentNotFoundException을 던진다")
    void countLikes_notFound_throwsCommentNotFoundException() {
        given(commentRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentLikeService.countLikes(5L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("특정 회원의 좋아요를 모두 제거할 때 각 좋아요마다 댓글 좋아요수를 감소시킨다")
    void removeAllByMemberId_decreasesCommentLikeCountForEachLike() {
        Member writer = member(1L);
        Post post = post(10L, writer);
        Comment commentA = comment(5L, post, writer);
        Comment commentB = comment(6L, post, writer);
        Member liker = member(2L);

        CommentLike likeA = CommentLike.of(commentA, liker);
        CommentLike likeB = CommentLike.of(commentB, liker);

        given(commentLikeRepository.findAllByMemberId(2L)).willReturn(List.of(likeA, likeB));

        commentLikeService.removeAllByMemberId(2L);

        then(commentLikeRepository).should().delete(likeA);
        then(commentLikeRepository).should().delete(likeB);
        then(commentRepository).should().decreaseLikeCount(5L);
        then(commentRepository).should().decreaseLikeCount(6L);
    }
}
