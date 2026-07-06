package com.tavemakers.surf.domain.score.repository;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalActivityScoreRepository extends JpaRepository<PersonalActivityScore, Long> {

    Optional<PersonalActivityScore> findByMemberId(Long memberId);

    List<PersonalActivityScore> findAllByMemberIdIn(List<Long> memberIds);

    List<PersonalActivityScore> findAllByTeamIdIn(List<Long> teamIds);

    /** 점수 갱신용 행 잠금 조회 — id 순 잠금으로 데드락 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.member.id in :memberIds order by s.id")
    List<PersonalActivityScore> findAllByMemberIdInForUpdate(@Param("memberIds") List<Long> memberIds);

    /** 점수 갱신용 행 잠금 조회 — id 순 잠금으로 데드락 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.team.id in :teamIds order by s.id")
    List<PersonalActivityScore> findAllByTeamIdInForUpdate(@Param("teamIds") List<Long> teamIds);

    /** 점수 갱신용 행 잠금 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.member.id = :memberId")
    Optional<PersonalActivityScore> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    void deleteByMemberId(Long memberId);

    void deleteByTeamId(Long teamId);

}
