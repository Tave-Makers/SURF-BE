package com.tavemakers.surf.application.score.query;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.exception.PersonalScoreNotFoundException;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
        List<PersonalActivityScore> scoreList = personalScoreRepository.findAllByMemberIdInForUpdate(memberIdList);
        validateAllFound(scoreList, memberIdList);
        return scoreList;
    }

    /** 여러 팀의 활동 점수 목록 조회 (점수 갱신용 — 행 잠금, 호출측 트랜잭션 필수) */
    public List<PersonalActivityScore> getTeamScoreListByIdsForUpdate(List<Long> teamIdList) {
        List<PersonalActivityScore> scoreList = personalScoreRepository.findAllByTeamIdInForUpdate(teamIdList);
        validateAllFound(scoreList, teamIdList);
        return scoreList;
    }

    /** 점수 행이 없는 대상이 섞여 있으면 예외 — 갱신 경로에서 대상 누락이 조용히 건너뛰어지는 것(silent no-op) 방지 */
    private void validateAllFound(List<PersonalActivityScore> scoreList, List<Long> requestedIdList) {
        if (scoreList.size() != new HashSet<>(requestedIdList).size()) {
            throw new PersonalScoreNotFoundException();
        }
    }

}
