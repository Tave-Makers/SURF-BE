package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.exception.PersonalScoreNotFoundException;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalScoreGetService {

    private final PersonalActivityScoreRepository personalScoreRepository;

    /** 회원의 개인 활동 점수 조회 */
    public PersonalActivityScore getPersonalScore(Long memberId) {
        return personalScoreRepository.findByMemberId(memberId)
                .orElseThrow(PersonalScoreNotFoundException::new);
    }

    /** 다수 회원의 개인 활동 점수 목록 조회 */
    public List<PersonalActivityScore> getPersonalScoreList(List<Long> memberIdList) {
        return personalScoreRepository.findAllByMemberIdIn(memberIdList);
    }

    public List<PersonalActivityScore> getTeamScoreList(List<Long> teamIdList) {
        return personalScoreRepository.findAllByTeamIdIn(teamIdList);
    }

}
