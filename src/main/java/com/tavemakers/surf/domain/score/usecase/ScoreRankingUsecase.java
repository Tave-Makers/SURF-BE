package com.tavemakers.surf.domain.score.usecase;

import com.tavemakers.surf.domain.activity.dto.response.TeamMemberScoreListResDTO;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.activity.service.ActivityRecordGetService;
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
import com.tavemakers.surf.domain.team.entity.TeamMember;
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
    private final ActivityRecordGetService activityRecordGetService;
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
                .collect(Collectors.toMap(Member::getId, m -> m));

        // 3. 누적 점수 조회
        List<PersonalActivityScore> scores = personalScoreGetService.getPersonalScoreListByIds(memberIds);
        Map<Long, BigDecimal> scoreMap = scores.stream()
                .collect(Collectors.toMap(s -> s.getMember().getId(), PersonalActivityScore::getScore));

        // 4. 상/벌점 집계 조회
        Map<Long, Map<ScoreType, BigDecimal>> aggregation = activityRecordGetService.getScoreAggregation(memberIds);

        // 5. DTO 조립 후 totalScore DESC 정렬
        List<MemberScoreRankingResDTO> dtoList = memberIds.stream()
                .filter(memberMap::containsKey)
                .map(id -> {
                    Member member = memberMap.get(id);
                    Part part = extractLatestPart(member);
                    BigDecimal rewardTotal = getAggregatedScore(aggregation, id, ScoreType.REWARD);
                    BigDecimal penaltyTotal = getAggregatedScore(aggregation, id, ScoreType.PENALTY).abs();
                    BigDecimal totalScore = scoreMap.getOrDefault(id, BigDecimal.ZERO);
                    return MemberScoreRankingResDTO.of(member, part, rewardTotal, penaltyTotal, totalScore);
                })
                .sorted(Comparator.comparing(MemberScoreRankingResDTO::totalScore).reversed())
                .toList();

        // 6. 수동 Slice 생성
        return MemberScoreRankingSliceResDTO.from(createSlice(dtoList, pageNum, pageSize));
    }

    /** 팀별 상/벌점 현황 조회 (무한스크롤) */
    public TeamScoreRankingSliceResDTO getTeamScoreRanking(Integer generation, int pageNum, int pageSize) {
        // 1. 팀 + 멤버 목록 조회
        List<Team> teams = teamGetService.getTeamsWithMembers(generation);
        if (teams.isEmpty()) {
            return TeamScoreRankingSliceResDTO.from(
                    new SliceImpl<>(List.of(), PageRequest.of(pageNum, pageSize), false));
        }

        // 2. 전체 팀 멤버 ID 수집
        List<Long> allMemberIds = teams.stream()
                .flatMap(team -> team.getTeamMembers().stream())
                .map(tm -> tm.getMember().getId())
                .distinct()
                .toList();

        // 3. 누적 점수 조회
        Map<Long, BigDecimal> scoreMap = Map.of();
        if (!allMemberIds.isEmpty()) {
            scoreMap = personalScoreGetService.getPersonalScoreListByIds(allMemberIds).stream()
                    .collect(Collectors.toMap(s -> s.getMember().getId(), PersonalActivityScore::getScore));
        }

        // 4. 상/벌점 집계 조회
        Map<Long, Map<ScoreType, BigDecimal>> aggregation =
                activityRecordGetService.getScoreAggregation(allMemberIds);

        // 5. 팀별 합산 및 DTO 조립
        final Map<Long, BigDecimal> finalScoreMap = scoreMap;
        List<TeamScoreRankingResDTO> dtoList = teams.stream()
                .map(team -> {
                    BigDecimal teamReward = BigDecimal.ZERO;
                    BigDecimal teamPenalty = BigDecimal.ZERO;
                    BigDecimal teamTotal = BigDecimal.ZERO;

                    for (TeamMember tm : team.getTeamMembers()) {
                        Long memberId = tm.getMember().getId();
                        teamReward = teamReward.add(getAggregatedScore(aggregation, memberId, ScoreType.REWARD));
                        teamPenalty = teamPenalty.add(getAggregatedScore(aggregation, memberId, ScoreType.PENALTY).abs());
                        teamTotal = teamTotal.add(finalScoreMap.getOrDefault(memberId, BigDecimal.ZERO));
                    }

                    return TeamScoreRankingResDTO.of(team, teamReward, teamPenalty, teamTotal);
                })
                .sorted(Comparator.comparing(TeamScoreRankingResDTO::totalScore).reversed())
                .toList();

        // 6. 수동 Slice 생성
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

        Map<Long, BigDecimal> scoreMap = personalScoreGetService.getPersonalScoreListByIds(memberIds).stream()
                .collect(Collectors.toMap(s -> s.getMember().getId(), PersonalActivityScore::getScore));

        Map<Long, Map<ScoreType, BigDecimal>> aggregation = activityRecordGetService.getScoreAggregation(memberIds);

        List<MemberScoreRankingResDTO> members = team.getTeamMembers().stream()
                .map(tm -> {
                    Member member = tm.getMember();
                    Part part = extractLatestPart(member);
                    BigDecimal rewardTotal = getAggregatedScore(aggregation, member.getId(), ScoreType.REWARD);
                    BigDecimal penaltyTotal = getAggregatedScore(aggregation, member.getId(), ScoreType.PENALTY).abs();
                    BigDecimal totalScore = scoreMap.getOrDefault(member.getId(), BigDecimal.ZERO);
                    return MemberScoreRankingResDTO.of(member, part, rewardTotal, penaltyTotal, totalScore);
                })
                .sorted(Comparator.comparing(MemberScoreRankingResDTO::totalScore).reversed())
                .toList();

        return TeamMemberScoreListResDTO.of(team.getId(), team.getName(), members);
    }

    /** 멤버의 최신 기수 Track에서 Part 추출 */
    private Part extractLatestPart(Member member) {
        return member.getTracks().stream()
                .max(Comparator.comparing(Track::getGeneration))
                .map(Track::getPart)
                .orElse(null);
    }

    /** 집계 결과에서 특정 멤버의 ScoreType별 합계 조회 */
    private BigDecimal getAggregatedScore(Map<Long, Map<ScoreType, BigDecimal>> aggregation,
                                           Long memberId, ScoreType scoreType) {
        return aggregation.getOrDefault(memberId, Map.of())
                .getOrDefault(scoreType, BigDecimal.ZERO);
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
