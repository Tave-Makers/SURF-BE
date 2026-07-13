package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.member.exception.MemberDismissNotAllowedException;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.member.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.service.MemberWithdrawService;
import com.tavemakers.surf.application.post.usecase.PostDeleteUsecase;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import com.tavemakers.surf.domain.team.service.TeamMemberCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

/**
 * MemberDismissUsecase.dismiss 오케스트레이션 단위 테스트.
 * 모든 협력자를 mock 처리해 (1) 제명 불가 상태의 조기 예외·부수효과 전무,
 * (2) 정상 경로의 호출 순서·데이터 전달(deletedPostIds 배선)을 겨눈다.
 * DataJpaTest 기반 완전성/롤백/팀정리 통합 테스트(MemberDismiss*Test)와 달리
 * 실제 리스너/DB 없이 오케스트레이션 로직 자체만 빠르게 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberDismissUsecaseTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CareerRepository careerRepository;
    @Mock
    private TrackRepository trackRepository;
    @Mock
    private MemberBlacklistCreateService memberBlacklistCreateService;
    @Mock
    private MemberWithdrawService memberWithdrawService;
    @Mock
    private TeamMemberCleanupService teamMemberCleanupService;
    @Mock
    private PostDeleteUsecase postDeleteUsecase;
    @Mock
    private PostLikeService postLikeService;
    @Mock
    private ScrapService scrapService;
    @Mock
    private CommentDeleteService commentDeleteService;
    @Mock
    private CommentLikeService commentLikeService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberDismissUsecase memberDismissUsecase;

    private Member member(Long id, MemberStatus status) {
        Member member = Member.builder().status(status).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("APPROVED가 아니면 예외를 던지고 블랙리스트 등록·정리·삭제 등 어떤 부수효과도 발생하지 않는다")
    void dismiss_whenNotApproved_throwsAndSkipsAllSideEffects() {
        Member member = member(1L, MemberStatus.WAITING);

        assertThatThrownBy(() -> memberDismissUsecase.dismiss(member, 999L))
                .isInstanceOf(MemberDismissNotAllowedException.class);

        then(memberBlacklistCreateService).shouldHaveNoInteractions();
        then(memberWithdrawService).shouldHaveNoInteractions();
        then(teamMemberCleanupService).shouldHaveNoInteractions();
        then(postDeleteUsecase).shouldHaveNoInteractions();
        then(postLikeService).shouldHaveNoInteractions();
        then(scrapService).shouldHaveNoInteractions();
        then(commentLikeService).shouldHaveNoInteractions();
        then(commentDeleteService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
        then(careerRepository).shouldHaveNoInteractions();
        then(trackRepository).shouldHaveNoInteractions();
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("APPROVED 회원 제명은 블랙리스트 등록부터 member row 삭제까지 정해진 순서로 진행되고, 삭제된 게시글 id가 댓글 정리로 정확히 전달된다")
    void dismiss_whenApproved_ordersFullPipelineAndWiresDeletedPostIdsThrough() {
        Member member = member(1L, MemberStatus.APPROVED);
        Set<Long> deletedPostIds = Set.of(100L, 200L);
        given(postDeleteUsecase.deleteAllOwnedBy(1L)).willReturn(deletedPostIds);
        given(careerRepository.findByMemberId(1L)).willReturn(List.of());
        given(trackRepository.findByMemberId(1L)).willReturn(List.of());

        memberDismissUsecase.dismiss(member, 999L);

        InOrder inOrder = inOrder(
                memberBlacklistCreateService, memberWithdrawService, teamMemberCleanupService,
                postDeleteUsecase, postLikeService, scrapService, commentLikeService,
                commentDeleteService, eventPublisher, careerRepository, trackRepository, memberRepository);

        inOrder.verify(memberBlacklistCreateService)
                .createIfAbsent(member, MemberBlacklistActionType.DISMISS, 999L);
        inOrder.verify(memberWithdrawService).disconnectMember(member);
        inOrder.verify(teamMemberCleanupService).cleanupOnDismiss(member);
        inOrder.verify(postDeleteUsecase).deleteAllOwnedBy(1L);
        inOrder.verify(postLikeService).unlikeAllByMemberId(1L);
        inOrder.verify(scrapService).removeAllByMemberId(1L);
        inOrder.verify(commentLikeService).removeAllByMemberId(1L);
        inOrder.verify(commentDeleteService).deleteAllByMemberId(1L, deletedPostIds);
        inOrder.verify(eventPublisher).publishEvent(new MemberDismissedEvent(1L));
        inOrder.verify(careerRepository).deleteAll(List.of());
        inOrder.verify(trackRepository).deleteAll(List.of());
        inOrder.verify(memberRepository).delete(member);
    }
}
