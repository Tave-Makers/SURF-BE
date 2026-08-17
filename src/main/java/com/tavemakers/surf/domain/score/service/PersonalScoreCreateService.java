package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.domain.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalScoreCreateService {

    private final PersonalActivityScoreRepository personalScoreRepository;

    /** 신규 회원들의 개인 활동 점수 초기화 저장 */
    public void savePersonalScores(List<Member> members) {
        if (members == null || members.isEmpty()) return;

        List<Long> memberIds = members.stream().map(Member::getId).distinct().toList();

        Set<Long> existing = personalScoreRepository.findAllByMemberIdIn(memberIds).stream()
                .map(s -> s.getMember().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<PersonalActivityScore> toSave = members.stream()
                .filter(m -> !existing.contains(m.getId()))
                .map(PersonalActivityScore::from)
                .toList();

        if (!toSave.isEmpty()) {
            personalScoreRepository.saveAll(toSave);
        }
    }

    /** 활동 기수 변경 시 승인 회원들의 개인 활동 점수를 현재 회원 구분(YB/OB)에 맞게 초기화한다. */
    public void resetPersonalScores(List<Member> members) {
        if (members == null || members.isEmpty()) return;

        List<Long> memberIds = members.stream().map(Member::getId).distinct().toList();
        Map<Long, PersonalActivityScore> existingByMemberId = personalScoreRepository.findAllByMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(s -> s.getMember().getId(), Function.identity()));

        List<PersonalActivityScore> upserts = members.stream()
                .map(member -> {
                    PersonalActivityScore score = existingByMemberId.get(member.getId());
                    if (score == null) {
                        return PersonalActivityScore.from(member);
                    }
                    score.resetForMember(member);
                    return score;
                })
                .toList();

        personalScoreRepository.saveAll(upserts);
    }

    /** 팀 활동 점수 초기화 저장 (중복 생성 방지) */
    public void saveTeamScore(Team team) {
        List<PersonalActivityScore> existing = personalScoreRepository.findAllByTeamIdIn(List.of(team.getId()));
        if (!existing.isEmpty()) return;

        PersonalActivityScore score = PersonalActivityScore.from(team);
        personalScoreRepository.save(score);
    }
}
