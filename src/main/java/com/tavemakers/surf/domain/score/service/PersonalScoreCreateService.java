package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import com.tavemakers.surf.domain.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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

    /** 팀 활동 점수 초기화 저장 */
    public void saveTeamScore(Team team) {
        PersonalActivityScore score = PersonalActivityScore.from(team);
        personalScoreRepository.save(score);
    }
}
