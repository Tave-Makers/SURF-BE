package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.domain.team.dto.request.TeamUpsertReqDTO;
import com.tavemakers.surf.domain.team.dto.response.*;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamMember;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotFoundException;
import com.tavemakers.surf.domain.team.exception.TeamLeaderNotInMemberException;
import com.tavemakers.surf.domain.team.exception.TeamMemberDuplicatedException;
import com.tavemakers.surf.domain.team.exception.TeamNotFoundException;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import com.tavemakers.surf.domain.member.dto.response.TrackResDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TrackRepository trackRepository;

    @Transactional(readOnly = true)
    public List<TeamGenerationSectionResDTO> getTeams(TeamType type) {
        List<TeamListResDTO> teams = teamRepository.findAllForAccordion(type).stream()
                .map(TeamListResDTO::from)
                .toList();

        Map<Integer, List<TeamListResDTO>> grouped = new LinkedHashMap<>();

        for (TeamListResDTO dto : teams) {
            grouped.computeIfAbsent(dto.generation(), k -> new ArrayList<>()).add(dto);
        }

        return grouped.entrySet().stream()
                .map(e -> new TeamGenerationSectionResDTO(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamDetailResDTO getTeamDetail(Long teamId) {
        Team team = teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        // 1) tracks 조회에 필요한 memberIds
        Set<Long> memberIdSet = new HashSet<>();
        memberIdSet.add(team.getLeader().getId());
        team.getTeamMembers().forEach(tm -> memberIdSet.add(tm.getMember().getId()));
        List<Long> memberIds = memberIdSet.stream().toList();

        // 2) Track을 한 번에 조회 (N+1 방지)
        Map<Long, List<Track>> trackMap = trackRepository.findAllByMemberIds(memberIds).stream()
                .collect(Collectors.groupingBy(t -> t.getMember().getId()));

        // 3) 팀장 DTO
        TeamDetailResDTO.MemberCardDTO leaderDto = toMemberCard(team.getLeader(), trackMap);

        // 4) members: 리더 제외 정렬(최신 기수 순 -> 이름 순)
        List<Member> members = team.getTeamMembers().stream()
                .map(TeamMember::getMember)
                .filter(m -> !m.getId().equals(team.getLeader().getId()))
                .sorted(memberComparator(trackMap))
                .toList();

        List<TeamDetailResDTO.MemberCardDTO> memberDtos = members.stream()
                .map(m -> toMemberCard(m, trackMap))
                .toList();

        return TeamDetailResDTO.from(team, leaderDto, memberDtos);
    }

    @Transactional
    public TeamResDTO createTeam(TeamUpsertReqDTO req) {

        ResolvedMembers resolved = resolveMembers(req);

        Team team = Team.of(
                req.generation(),
                req.type(),
                req.name(),
                req.description(),
                resolved.leader()
        );

        // leader 제외하고 추가
        for (Member m : resolved.members()) {
            if (!m.getId().equals(resolved.leader().getId())) {
                team.addMember(m);
            }
        }

        Team saved = teamRepository.save(team);

        return TeamResDTO.from(saved);
    }

    @Transactional
    public TeamResDTO updateTeam(Long teamId, TeamUpsertReqDTO req) {
        Team team = teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        ResolvedMembers resolved = resolveMembers(req);

        // 1) 기본 정보(전체) 반영
        team.changeInfo(req.generation(), req.type(), req.name(), req.description());
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

        return TeamResDTO.from(team);
    }


    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        teamRepository.delete(team);
    }

    private ResolvedMembers resolveMembers(TeamUpsertReqDTO req) {
        List<Long> raw = req.memberIds();

        // 1) 중복 제거
        List<Long> distinctMemberIds = req.memberIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctMemberIds.size() != raw.size()) {
            throw new TeamMemberDuplicatedException();
        }

        // 2) 팀장은 팀원 목록에 반드시 포함
        if (!distinctMemberIds.contains(req.leaderMemberId())) {
            throw new TeamLeaderNotInMemberException();
        }

        // 3) 멤버 조회
        List<Member> members = memberRepository.findAllById(distinctMemberIds);
        if (members.size() != distinctMemberIds.size()) {
            throw new MemberNotFoundException();
        }

        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        Member leader = memberMap.get(req.leaderMemberId());
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

    private TeamDetailResDTO.MemberCardDTO toMemberCard(Member m, Map<Long, List<Track>> trackMap) {
        List<TrackResDTO> tracks = trackMap.getOrDefault(m.getId(), List.of()).stream()
                .map(TrackResDTO::from)
                .toList();

        return new TeamDetailResDTO.MemberCardDTO(
                m.getId(),
                m.getName(),
                m.getProfileImageUrl(),
                tracks
        );
    }

    private Comparator<Member> memberComparator(Map<Long, List<Track>> trackMap) {
        Comparator<Integer> generationDesc = Comparator.nullsLast(Comparator.reverseOrder());

        return Comparator
                .comparing((Member m) -> mainGeneration(m.getId(), trackMap), generationDesc)
                .thenComparing(Member::getName, Comparator.nullsLast(String::compareTo));
    }

    private Integer mainGeneration(Long memberId, Map<Long, List<Track>> trackMap) {
        return trackMap.getOrDefault(memberId, List.of()).stream()
                .map(Track::getGeneration)
                .min(Integer::compareTo)
                .orElse(null);
    }
}