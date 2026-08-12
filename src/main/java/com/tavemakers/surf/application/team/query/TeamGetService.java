package com.tavemakers.surf.application.team.query;

import com.tavemakers.surf.application.member.query.TrackGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.entity.TeamMember;
import com.tavemakers.surf.domain.team.entity.TeamType;
import com.tavemakers.surf.domain.team.exception.TeamNotFoundException;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import com.tavemakers.surf.presentation.member.dto.response.TrackResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamDetailResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamGenerationSectionResDTO;
import com.tavemakers.surf.presentation.team.dto.response.TeamListResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 팀 read-model 조립. 팀 조회 계약(엔티티 반환)과, 타 도메인(track) 조회를 오케스트레이션하여
 * 표현형(DTO)을 구성하는 로직을 함께 제공한다. 트랜잭션(readOnly) 경계는 호출자(TeamUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamGetService {

    private final TeamRepository teamRepository;
    private final TrackGetService trackGetService;

    /** 기수별 팀 목록 조회 (멤버 포함) */
    public List<Team> getTeamsWithMembers(Integer generation) {
        return teamRepository.findTeamsWithMembers(generation);
    }

    /** 팀 단건 조회 (멤버 포함) */
    public Team getTeamWithMembers(Long teamId) {
        return teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);
    }

    /** 팀 목록 조회 (기수별 섹션으로 구성) */
    public List<TeamGenerationSectionResDTO> getTeams(TeamType type, Integer generation) {
        List<TeamListResDTO> teams = teamRepository.findAllForAccordion(type, generation).stream()
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

    /** 팀 상세 조회 */
    public TeamDetailResDTO getTeamDetail(Long teamId) {
        Team team = teamRepository.findDetailBaseById(teamId)
                .orElseThrow(TeamNotFoundException::new);

        // 1) tracks 조회에 필요한 memberIds
        Set<Long> memberIdSet = new HashSet<>();
        memberIdSet.add(team.getLeader().getId());
        team.getTeamMembers().forEach(tm -> memberIdSet.add(tm.getMember().getId()));
        List<Long> memberIds = memberIdSet.stream().toList();

        // 2) Track을 한 번에 조회 (N+1 방지)
        Map<Long, List<Track>> trackMap = trackGetService.getTracksByMemberIds(memberIds).stream()
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
