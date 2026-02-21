package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.exception.PersonalScoreNotFoundException;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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

    /** 여러 회원의 개인 활동 점수 목록 조회 */
    public List<PersonalActivityScore> getPersonalScoreListByIds(List<Long> memberIdList) {
        return personalScoreRepository.findAllByMemberIdIn(memberIdList);
    }

    /** 여러 팀의 개인 활동 점수 목록 조회 */
    public List<PersonalActivityScore> getTeamScoreListByIds(List<Long> teamIdList) {
        return personalScoreRepository.findAllByTeamIdIn(teamIdList);
    }

}
