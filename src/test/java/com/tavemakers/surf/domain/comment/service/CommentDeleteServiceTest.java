package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.repository.CommentLikeRepository;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.service.support.PostCommentCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * CommentDeleteService 단위 테스트 — 게시글 삭제 시 자식/연관 데이터 처리 순서와
 * 회원 탈퇴 시 "이미 삭제 예정인 게시글"을 건너뛰는 분기 로직을 겨냥한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentDeleteServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private CommentMentionRepository commentMentionRepository;
    @Mock
    private PostCommentCountService postCommentCountService;

    private CommentDeleteService commentDeleteService;

    @BeforeEach
    void setUp() {
        commentDeleteService = new CommentDeleteService(
                commentRepository, commentLikeRepository, commentMentionRepository, postCommentCountService);
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
    @DisplayName("게시글 전체 삭제 시 좋아요 → 멘션 → 대댓글 → 루트댓글 순서로 삭제한다")
    void deleteAllByPostId_deletesInDependencyOrder() {
        commentDeleteService.deleteAllByPostId(10L);

        InOrder order = inOrder(commentLikeRepository, commentMentionRepository, commentRepository);
        order.verify(commentLikeRepository).deleteAllByPostId(10L);
        order.verify(commentMentionRepository).deleteAllByPostId(10L);
        order.verify(commentRepository).deleteRepliesByPostId(10L);
        order.verify(commentRepository).deleteRootCommentsByPostId(10L);
    }

    @Test
    @DisplayName("이미 삭제 예정인 게시글의 댓글은 건너뛰고, 그렇지 않은 댓글만 단건 삭제한다")
    void deleteAllByMemberId_skipsCommentsInAlreadyDeletedPosts() {
        Member writer = member(1L);
        Post deletedPost = post(100L, writer);
        Post keptPost = post(200L, writer);
        Comment commentInDeletedPost = comment(1L, deletedPost, writer);
        Comment commentInKeptPost = comment(2L, keptPost, writer);

        given(commentRepository.findAllByMemberId(1L))
                .willReturn(List.of(commentInDeletedPost, commentInKeptPost));

        commentDeleteService.deleteAllByMemberId(1L, Set.of(100L));

        // 삭제 예정 게시글(100L)의 댓글은 건드리지 않는다
        then(commentRepository).should(never()).detachChildren(1L);
        then(commentRepository).should(never()).delete(commentInDeletedPost);

        // 유지되는 게시글(200L)의 댓글은 연관 데이터까지 모두 삭제한다
        then(commentRepository).should().detachChildren(2L);
        then(commentLikeRepository).should().deleteAllByComment(commentInKeptPost);
        then(commentMentionRepository).should().deleteAllByComment(commentInKeptPost);
        then(commentRepository).should().delete(commentInKeptPost);
        then(postCommentCountService).should().decrease(200L);
        then(postCommentCountService).should(never()).decrease(100L);
    }

    @Test
    @DisplayName("댓글 단건 강제 삭제는 자식 detach 후 연관 데이터·댓글을 삭제하고 게시글 댓글수를 감소시킨다")
    void deleteComment_detachesChildrenThenDeletesAssociatedDataAndDecreasesCount() {
        Member writer = member(1L);
        Post post = post(10L, writer);
        Comment comment = comment(5L, post, writer);

        commentDeleteService.deleteComment(comment);

        InOrder order = inOrder(commentRepository, commentLikeRepository, commentMentionRepository, postCommentCountService);
        order.verify(commentRepository).detachChildren(5L);
        order.verify(commentLikeRepository).deleteAllByComment(comment);
        order.verify(commentMentionRepository).deleteAllByComment(comment);
        order.verify(commentRepository).delete(comment);
        order.verify(postCommentCountService).decrease(10L);
    }
}
