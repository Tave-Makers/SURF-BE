package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotFoundException;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotInMemberException;
import com.tavemakers.surf.domain.team.exception.TeamMemberDuplicatedException;
import com.tavemakers.surf.domain.team.event.TeamDeletedEvent;
import com.tavemakers.surf.domain.team.exception.TeamNotFoundException;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 팀 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(TeamUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberGetService memberGetService;
    private final PersonalScoreCreateService personalScoreCreateService;
    private final ApplicationEventPublisher eventPublisher;

    /** 팀 생성 */
    public Team createTeam(Integer generation, TeamType type, String name, String description,
                           Long leaderMemberId, List<Long> memberIds) {

        ResolvedMembers resolved = resolveMembers(memberIds, leaderMemberId);

        Team team = Team.of(
                generation,
                type,
                name,
                description,
                resolved.leader()
        );

        // leader 제외하고 추가
        for (Member m : resolved.members()) {
            if (!m.getId().equals(resolved.leader().getId())) {
                team.addMember(m);
            }
        }

        Team saved = teamRepository.save(team);
        personalScoreCreateService.saveTeamScore(saved);

        return saved;
    }

    /** 팀 수정 */
    public Team updateTeam(Long teamId, Integer generation, TeamType type, String name, String description,
                           Long leaderMemberId, List<Long> memberIds) {
        Team team = teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        ResolvedMembers resolved = resolveMembers(memberIds, leaderMemberId);

        // 1) 기본 정보(전체) 반영
        team.changeInfo(generation, type, name, description);
        team.changeLeader(resolved.leader());

        // 2) 팀원을 요청 memberIds와 동일하게 맞추기
        // 현재 멤버 id set
        Set<Long> current = team.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .collect(Collectors.toSet());

        // 요청 멤버 id set
        Set<Long> target = resolved.memberIdsSet();

        // 2-1) 추가해야 할 팀원: target - current
        for (Long memberId : target) {
            if (!current.contains(memberId)) {
                team.addMember(resolved.memberMap().get(memberId));
            }
        }

        // 2-2) 제거해야 할 팀원: current - target
        for (Long memberId : current) {
            if (!target.contains(memberId)) {
                team.removeMember(memberId);
            }
        }

        return team;
    }


    /** 팀 삭제 */
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        // 팀 부속 데이터(활동기록/팀 점수)를 동기 리스너가 같은 트랜잭션에서 먼저 정리한다
        // (TeamMemberCleanupService.cleanupOnDismiss 와 동일 순서 — 미발행 시 FK 위반/고아 행)
        eventPublisher.publishEvent(new TeamDeletedEvent(team.getId()));
        teamRepository.delete(team);
    }

    private ResolvedMembers resolveMembers(List<Long> memberIds, Long leaderMemberId) {
        List<Long> raw = memberIds;

        // 1) 중복 제거
        List<Long> distinctMemberIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctMemberIds.size() != raw.size()) {
            throw new TeamMemberDuplicatedException();
        }

        // 2) 팀장은 팀원 목록에 반드시 포함
        if (!distinctMemberIds.contains(leaderMemberId)) {
            throw new TeamLeaderNotInMemberException();
        }

        // 3) 멤버 조회
        List<Member> members = memberGetService.getMembersByIds(distinctMemberIds);
        if (members.size() != distinctMemberIds.size()) {
            throw new MemberNotFoundException();
        }

        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        Member leader = memberMap.get(leaderMemberId);
        if (leader == null)
            throw new TeamLeaderNotFoundException();

        return new ResolvedMembers(members, leader, memberMap, new HashSet<>(distinctMemberIds));
    }

    private record ResolvedMembers(
            List<Member> members,
            Member leader,
            Map<Long, Member> memberMap,
            Set<Long> memberIdsSet
    ) {}
}
