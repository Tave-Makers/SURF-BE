package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentMention;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * CommentMentionService 단위 테스트 — 멘션 회원 ID 목록의 null/빈값/중복 처리 분기를 겨냥한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentMentionServiceTest {

    @Mock
    private CommentMentionRepository commentMentionRepository;
    @Mock
    private MemberGetService memberGetService;

    private CommentMentionService commentMentionService;

    @BeforeEach
    void setUp() {
        commentMentionService = new CommentMentionService(commentMentionRepository, memberGetService);
    }

    private Member member(long id) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("provider-" + id)
                .name("회원" + id)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Comment comment() {
        Member writer = member(1L);
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
        Comment comment = Comment.root(post, writer, "댓글");
        ReflectionTestUtils.setField(comment, "id", 5L);
        return comment;
    }

    @Test
    @DisplayName("멘션 ID 목록이 null이면 빈 리스트를 반환하고 회원 조회·저장을 하지 않는다")
    void createMentions_nullIds_returnsEmptyWithoutTouchingRepository() {
        List<CommentMention> result = commentMentionService.createMentions(comment(), null);

        assertThat(result).isEmpty();
        then(memberGetService).should(never()).getMembersByIds(anyList());
        then(commentMentionRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("멘션 ID 목록이 빈 리스트이면 빈 리스트를 반환하고 회원 조회·저장을 하지 않는다")
    void createMentions_emptyIds_returnsEmptyWithoutTouchingRepository() {
        List<CommentMention> result = commentMentionService.createMentions(comment(), Collections.emptyList());

        assertThat(result).isEmpty();
        then(memberGetService).should(never()).getMembersByIds(anyList());
        then(commentMentionRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("중복된 멘션 ID는 제거한 뒤 회원을 조회한다")
    void createMentions_duplicateIds_dedupesBeforeFetchingMembers() {
        Comment comment = comment();
        given(memberGetService.getMembersByIds(List.of(2L, 3L))).willReturn(List.of(member(2L), member(3L)));
        given(commentMentionRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        commentMentionService.createMentions(comment, List.of(2L, 3L, 2L));

        then(memberGetService).should().getMembersByIds(List.of(2L, 3L));
    }

    @Test
    @DisplayName("조회된 회원마다 CommentMention을 생성해 일괄 저장하고 저장 결과를 반환한다")
    void createMentions_savesMentionPerMemberAndReturnsSavedResult() {
        Comment comment = comment();
        Member mentioned = member(2L);
        given(memberGetService.getMembersByIds(List.of(2L))).willReturn(List.of(mentioned));
        given(commentMentionRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<CommentMention> result = commentMentionService.createMentions(comment, List.of(2L));

        ArgumentCaptor<List<CommentMention>> captor = ArgumentCaptor.forClass(List.class);
        then(commentMentionRepository).should().saveAll(captor.capture());
        List<CommentMention> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getComment()).isEqualTo(comment);
        assertThat(saved.get(0).getMentionedMember()).isEqualTo(mentioned);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("댓글 삭제 시 해당 댓글의 멘션 전체 삭제를 리포지토리에 위임한다")
    void deleteAllByComment_delegatesToRepository() {
        Comment comment = comment();

        commentMentionService.deleteAllByComment(comment);

        then(commentMentionRepository).should().deleteAllByComment(comment);
    }
}
