package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamMember;
import com.tavemakers.surf.domain.team.event.TeamDeletedEvent;
import com.tavemakers.surf.domain.team.repository.TeamMemberRepository;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 회원 제명 시 팀 정리 — 리더 위임/팀 삭제 순서 의존이 있어 이벤트가 아닌
 * 명시적 호출로 오케스트레이션된다 (호출자 트랜잭션에 참여).
 * 팀이 삭제되는 경우 부속 데이터(활동기록/점수) 정리는 TeamDeletedEvent로 위임한다.
 */
@Service
@RequiredArgsConstructor
public class TeamMemberCleanupService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 제명 대상 회원의 팀 소속 정리 — 리더면 위임하고, 남은 멤버가 없으면 팀 자체를 삭제 */
    public void cleanupOnDismiss(Member member) {
        for (Team team : teamRepository.findAllByMemberIdForDismissal(member.getId())) {
            List<Member> otherMembers = team.getTeamMembers().stream()
                    .map(TeamMember::getMember)
                    .filter(teamMember -> !teamMember.getId().equals(member.getId()))
                    .toList();

            if (team.getLeader().getId().equals(member.getId())) {
                if (otherMembers.isEmpty()) {
                    eventPublisher.publishEvent(new TeamDeletedEvent(team.getId()));
                    teamRepository.delete(team);
                    continue;
                }
                team.changeLeader(otherMembers.get(0));
            }

            team.removeMember(member.getId());
        }

        teamMemberRepository.deleteAllByMemberId(member.getId());
    }
}
