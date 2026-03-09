package com.tavemakers.surf.domain.score.usecase;

import com.tavemakers.surf.domain.activity.dto.activityRecord.response.TeamMemberScoreListResDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.score.dto.response.MemberScoreRankingResDTO;
import com.tavemakers.surf.domain.score.dto.response.MemberScoreRankingSliceResDTO;
import com.tavemakers.surf.domain.score.dto.response.TeamScoreRankingResDTO;
import com.tavemakers.surf.domain.score.dto.response.TeamScoreRankingSliceResDTO;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.service.TeamGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreRankingUsecase {

    private final MemberGetService memberGetService;
    private final PersonalScoreGetService personalScoreGetService;
    private final TeamGetService teamGetService;

    /** 개인별 상/벌점 현황 조회 (무한스크롤) */
    public MemberScoreRankingSliceResDTO getMemberScoreRanking(int pageNum, int pageSize) {
        // 1. 활동 멤버 ID 목록 조회
        List<Long> memberIds = memberGetService.getActiveMemberIdsExcludeStatus(MemberStatus.WITHDRAWN);
        if (memberIds.isEmpty()) {
            return MemberScoreRankingSliceResDTO.from(
                    new SliceImpl<>(List.of(), PageRequest.of(pageNum, pageSize), false));
        }

        // 2. Member 엔티티 목록 조회
        List<Member> members = memberGetService.findMembersByIds(memberIds);
        Map<Long, Member> memberMap = members.stream()
                .collect(Collectors.toMap(Member::getId, m -> m, (a, b) -> a));

        // 3. 누적 점수 조회
        Map<Long, PersonalActivityScore> scoreMap = personalScoreGetService.getPersonalScoreListByIds(memberIds).stream()
                .collect(Collectors.toMap(s -> s.getMember().getId(), s -> s, (a, b) -> a));

        // 4. DTO 조립 후 totalScore DESC 정렬
        List<MemberScoreRankingResDTO> dtoList = memberIds.stream()
                .filter(memberMap::containsKey)
                .map(id -> {
                    Member member = memberMap.get(id);
                    Part part = extractLatestPart(member);
                    PersonalActivityScore score = scoreMap.get(id);
                    BigDecimal rewardTotal = score != null ? score.getRewardPrefixSum() : BigDecimal.ZERO;
                    BigDecimal penaltyTotal = score != null ? score.getPenaltyPrefixSum().abs() : BigDecimal.ZERO;
                    BigDecimal totalScore = score != null ? score.getScore() : BigDecimal.ZERO;
                    return MemberScoreRankingResDTO.of(member, part, rewardTotal, penaltyTotal, totalScore);
                })
                .sorted(Comparator.comparing(MemberScoreRankingResDTO::totalScore).reversed())
                .toList();

        // 5. 수동 Slice 생성
        return MemberScoreRankingSliceResDTO.from(createSlice(dtoList, pageNum, pageSize));
    }

    /** 팀별 상/벌점 현황 조회 (무한스크롤) */
    public TeamScoreRankingSliceResDTO getTeamScoreRanking(Integer generation, int pageNum, int pageSize) {
        // 1. 팀 목록 조회
        List<Team> teams = teamGetService.getTeamsWithMembers(generation);
        if (teams.isEmpty()) {
            return TeamScoreRankingSliceResDTO.from(
                    new SliceImpl<>(List.of(), PageRequest.of(pageNum, pageSize), false));
        }

        // 2. 팀 ID 수집
        List<Long> teamIds = teams.stream()
                .map(Team::getId)
                .toList();

        // 3. 팀 자체 누적 점수 조회
        Map<Long, PersonalActivityScore> teamScoreMap =
                personalScoreGetService.getTeamScoreListByIds(teamIds).stream()
                        .collect(Collectors.toMap(s -> s.getTeam().getId(), s -> s, (a, b) -> a));

        // 4. DTO 조립 후 totalScore DESC 정렬
        List<TeamScoreRankingResDTO> dtoList = teams.stream()
                .map(team -> {
                    PersonalActivityScore teamScore = teamScoreMap.get(team.getId());
                    BigDecimal rewardTotal = teamScore != null ? teamScore.getRewardPrefixSum() : BigDecimal.ZERO;
                    BigDecimal penaltyTotal = teamScore != null ? teamScore.getPenaltyPrefixSum().abs() : BigDecimal.ZERO;
                    BigDecimal totalScore = teamScore != null ? teamScore.getScore() : BigDecimal.ZERO;
                    return TeamScoreRankingResDTO.of(team, rewardTotal, penaltyTotal, totalScore);
                })
                .sorted(Comparator.comparing(TeamScoreRankingResDTO::totalScore).reversed())
                .toList();

        // 5. 수동 Slice 생성
        return TeamScoreRankingSliceResDTO.from(createSlice(dtoList, pageNum, pageSize));
    }

    /** 특정 팀의 멤버별 점수 현황 조회 */
    public TeamMemberScoreListResDTO getTeamMemberScores(Long teamId) {
        Team team = teamGetService.getTeamWithMembers(teamId);

        List<Long> memberIds = team.getTeamMembers().stream()
                .map(tm -> tm.getMember().getId())
                .toList();

        if (memberIds.isEmpty()) {
            return TeamMemberScoreListResDTO.of(team.getId(), team.getName(), List.of());
        }

        Map<Long, PersonalActivityScore> scoreMap = personalScoreGetService.getPersonalScoreListByIds(memberIds).stream()
                .collect(Collectors.toMap(s -> s.getMember().getId(), s -> s, (a, b) -> a));

        List<MemberScoreRankingResDTO> members = team.getTeamMembers().stream()
                .map(tm -> {
                    Member member = tm.getMember();
                    Part part = extractLatestPart(member);
                    PersonalActivityScore score = scoreMap.get(member.getId());
                    BigDecimal rewardTotal = score != null ? score.getRewardPrefixSum() : BigDecimal.ZERO;
                    BigDecimal penaltyTotal = score != null ? score.getPenaltyPrefixSum().abs() : BigDecimal.ZERO;
                    BigDecimal totalScore = score != null ? score.getScore() : BigDecimal.ZERO;
                    return MemberScoreRankingResDTO.of(member, part, rewardTotal, penaltyTotal, totalScore);
                })
                .sorted(Comparator.comparing(MemberScoreRankingResDTO::totalScore).reversed())
                .toList();

        return TeamMemberScoreListResDTO.of(team.getId(), team.getName(), members);
    }

    /** 멤버의 최신 기수 Track에서 Part를 추출합니다. 조회 가능한 Track이 없으면 null을 반환합니다. */
    private Part extractLatestPart(Member member) {
        return member.getTracks().stream()
                .max(Comparator.comparing(Track::getGeneration))
                .map(Track::getPart)
                .orElse(null);
    }

    /** 리스트에서 수동 Slice 생성 (offset/limit 기반) */
    private <T> SliceImpl<T> createSlice(List<T> list, int pageNum, int pageSize) {
        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, list.size());

        if (start >= list.size()) {
            return new SliceImpl<>(List.of(), PageRequest.of(pageNum, pageSize), false);
        }

        List<T> content = list.subList(start, end);
        boolean hasNext = end < list.size();
        return new SliceImpl<>(content, PageRequest.of(pageNum, pageSize), hasNext);
    }

}
