package com.tavemakers.surf.domain.member.usecase;

import com.tavemakers.surf.domain.activity.repository.ActivityRecordRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.comment.service.CommentDeleteService;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.letter.repository.LetterRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.MemberDismissNotAllowedException;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import com.tavemakers.surf.domain.member.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.service.MemberWithdrawService;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.like.PostLikeService;
import com.tavemakers.surf.domain.post.service.post.PostDeleteUsecase;
import com.tavemakers.surf.domain.post.service.search.RecentSearchService;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamMember;
import com.tavemakers.surf.domain.team.repository.TeamMemberRepository;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberDismissUsecase {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CareerRepository careerRepository;
    private final TrackRepository trackRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final PersonalActivityScoreRepository personalActivityScoreRepository;
    private final LetterRepository letterRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final MemberBlacklistCreateService memberBlacklistCreateService;
    private final MemberWithdrawService memberWithdrawService;
    private final RecentSearchService recentSearchService;
    private final PostRepository postRepository;
    private final PostDeleteUsecase postDeleteUsecase;
    private final PostLikeService postLikeService;
    private final ScrapService scrapService;
    private final CommentDeleteService commentDeleteService;
    private final CommentLikeService commentLikeService;

    /** 회원 제명 오케스트레이션 — 블랙리스트 등록, 연결 해제, 데이터 정리 */
    @Transactional
    public void dismiss(Member member, Long processedBy) {
        validateDismissible(member);

        memberBlacklistCreateService.createIfAbsent(member, MemberBlacklistActionType.DISMISS, processedBy);
        memberWithdrawService.disconnectMember(member);
        recentSearchService.clearAll(member.getId());

        cleanupTeams(member);
        teamMemberRepository.deleteAllByMemberId(member.getId());

        Set<Long> deletedPostIds = deleteOwnedPosts(member.getId());
        postLikeService.unlikeAllByMemberId(member.getId());
        scrapService.removeAllByMemberId(member.getId());
        commentLikeService.removeAllByMemberId(member.getId());
        commentDeleteService.deleteAllByMemberId(member.getId(), deletedPostIds);
        commentMentionRepository.deleteAllByMentionedMemberId(member.getId());

        letterRepository.deleteByMemberId(member.getId());
        memberBadgeRepository.deleteByMemberId(member.getId());
        activityRecordRepository.deleteByMemberId(member.getId());
        notificationRepository.deleteByMemberId(member.getId());
        deviceTokenRepository.deleteByMemberId(member.getId());
        personalActivityScoreRepository.deleteByMemberId(member.getId());
        careerRepository.deleteAll(careerRepository.findByMemberId(member.getId()));
        trackRepository.deleteAll(trackRepository.findByMemberId(member.getId()));
        memberRepository.delete(member);
    }

    private void validateDismissible(Member member) {
        if (member.getStatus() != MemberStatus.APPROVED) {
            throw new MemberDismissNotAllowedException();
        }
    }

    private void cleanupTeams(Member member) {
        for (Team team : teamRepository.findAllByMemberIdForDismissal(member.getId())) {
            List<Member> otherMembers = team.getTeamMembers().stream()
                    .map(TeamMember::getMember)
                    .filter(teamMember -> !teamMember.getId().equals(member.getId()))
                    .toList();

            if (team.getLeader().getId().equals(member.getId())) {
                if (otherMembers.isEmpty()) {
                    activityRecordRepository.deleteByTeamId(team.getId());
                    personalActivityScoreRepository.deleteByTeamId(team.getId());
                    teamRepository.delete(team);
                    continue;
                }
                team.changeLeader(otherMembers.get(0));
            }

            team.removeMember(member.getId());
        }
    }

    private Set<Long> deleteOwnedPosts(Long memberId) {
        List<Post> posts = postRepository.findAllByMemberId(memberId);

        for (Post post : posts) {
            postDeleteUsecase.forceDeletePost(post);
        }

        return posts.stream().map(Post::getId).collect(Collectors.toSet());
    }
}
