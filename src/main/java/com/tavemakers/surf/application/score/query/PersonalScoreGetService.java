package com.tavemakers.surf.application.score.query;

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

    /** 여러 회원의 개인 활동 점수 목록 조회 */
    public List<PersonalActivityScore> getPersonalScoreListByIds(List<Long> memberIdList) {
        return personalScoreRepository.findAllByMemberIdIn(memberIdList);
    }

    /** 여러 팀의 활동 점수 목록 조회 */
    public List<PersonalActivityScore> getTeamScoreListByIds(List<Long> teamIdList) {
        return personalScoreRepository.findAllByTeamIdIn(teamIdList);
    }

    /** 회원의 개인 활동 점수 조회 (점수 갱신용 — 행 잠금, 호출측 트랜잭션 필수) */
    public PersonalActivityScore getPersonalScoreForUpdate(Long memberId) {
        return personalScoreRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(PersonalScoreNotFoundException::new);
    }

    /** 여러 회원의 개인 활동 점수 목록 조회 (점수 갱신용 — 행 잠금, 호출측 트랜잭션 필수) */
    public List<PersonalActivityScore> getPersonalScoreListByIdsForUpdate(List<Long> memberIdList) {
        return personalScoreRepository.findAllByMemberIdInForUpdate(memberIdList);
    }

    /** 여러 팀의 활동 점수 목록 조회 (점수 갱신용 — 행 잠금, 호출측 트랜잭션 필수) */
    public List<PersonalActivityScore> getTeamScoreListByIdsForUpdate(List<Long> teamIdList) {
        return personalScoreRepository.findAllByTeamIdInForUpdate(teamIdList);
    }

}
