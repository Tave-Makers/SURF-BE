package com.tavemakers.surf.domain.member.application.usecase;

import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.domain.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.domain.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.member.domain.exception.MemberDismissNotAllowedException;
import com.tavemakers.surf.domain.member.domain.repository.CareerRepository;
import com.tavemakers.surf.domain.member.domain.repository.MemberRepository;
import com.tavemakers.surf.domain.member.domain.repository.TrackRepository;
import com.tavemakers.surf.domain.member.domain.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.domain.service.MemberWithdrawService;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.post.PostDeleteUsecase;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import com.tavemakers.surf.domain.team.service.TeamMemberCleanupService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 제명 오케스트레이션 (D1 설계 — docs/refactoring-plan.md).
 *
 * <p>제명은 "전부 성공 또는 전부 롤백"이어야 하므로 단일 트랜잭션을 유지한다.
 * <ul>
 *   <li>순서·데이터 의존이 있는 정리(팀 리더 위임, 게시글 삭제 → deletedPostIds → 댓글 정리,
 *       좋아요/스크랩 카운트 정리)는 여기서 명시적으로 호출한다.</li>
 *   <li>독립적인 정리(멘션/쪽지/배지/활동기록/알림/점수)는 {@link MemberDismissedEvent}를 받은
 *       각 도메인의 동기 리스너가 같은 트랜잭션에서 수행한다 — member가 해당 도메인들을
 *       컴파일 타임에 의존하지 않는다.</li>
 *   <li>외부 효과(Kakao/Apple 연결 해제, Redis 검색기록)는 AFTER_COMMIT 리스너가 커밋 후 수행한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MemberDismissUsecase {

    private final MemberRepository memberRepository;
    private final CareerRepository careerRepository;
    private final TrackRepository trackRepository;
    private final MemberBlacklistCreateService memberBlacklistCreateService;
    private final MemberWithdrawService memberWithdrawService;
    private final TeamMemberCleanupService teamMemberCleanupService;
    private final PostDeleteUsecase postDeleteUsecase;
    private final PostLikeService postLikeService;
    private final ScrapService scrapService;
    private final CommentDeleteService commentDeleteService;
    private final CommentLikeService commentLikeService;
    private final ApplicationEventPublisher eventPublisher;

    /** 회원 제명 오케스트레이션 — 블랙리스트 등록, 연결 해제, 데이터 정리 */
    @Transactional
    public void dismiss(Member member, Long processedBy) {
        validateDismissible(member);

        memberBlacklistCreateService.createIfAbsent(member, MemberBlacklistActionType.DISMISS, processedBy);
        memberWithdrawService.disconnectMember(member); // 외부 연결 해제 — AFTER_COMMIT 리스너가 수행

        teamMemberCleanupService.cleanupOnDismiss(member);

        Set<Long> deletedPostIds = postDeleteUsecase.deleteAllOwnedBy(member.getId());
        postLikeService.unlikeAllByMemberId(member.getId());
        scrapService.removeAllByMemberId(member.getId());
        commentLikeService.removeAllByMemberId(member.getId());
        commentDeleteService.deleteAllByMemberId(member.getId(), deletedPostIds);

        // 독립 정리(멘션/쪽지/배지/활동기록/알림/점수)는 동기 리스너가 같은 트랜잭션에서 수행
        eventPublisher.publishEvent(new MemberDismissedEvent(member.getId()));

        careerRepository.deleteAll(careerRepository.findByMemberId(member.getId()));
        trackRepository.deleteAll(trackRepository.findByMemberId(member.getId()));
        memberRepository.delete(member);
    }

    private void validateDismissible(Member member) {
        if (member.getStatus() != MemberStatus.APPROVED) {
            throw new MemberDismissNotAllowedException();
        }
    }
}
