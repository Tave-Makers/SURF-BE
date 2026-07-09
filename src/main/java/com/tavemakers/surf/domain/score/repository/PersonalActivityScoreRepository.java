package com.tavemakers.surf.domain.score.repository;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    /** 점수 갱신용 행 잠금 조회 — 경합 트랜잭션이 모두 같은 인덱스 경로로 잠가 상호 순서가 일관됨 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.member.id in :memberIds order by s.id")
    List<PersonalActivityScore> findAllByMemberIdInForUpdate(@Param("memberIds") List<Long> memberIds);

    /** 점수 갱신용 행 잠금 조회 — 경합 트랜잭션이 모두 같은 인덱스 경로로 잠가 상호 순서가 일관됨 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.team.id in :teamIds order by s.id")
    List<PersonalActivityScore> findAllByTeamIdInForUpdate(@Param("teamIds") List<Long> teamIds);

    /** 점수 갱신용 행 잠금 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PersonalActivityScore s where s.member.id = :memberId")
    Optional<PersonalActivityScore> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    // 파생 delete(SELECT 후 개별 remove 큐잉)는 flush가 지연되어 다른 리스너의
    // clearAutomatically에 의해 취소될 수 있으므로 벌크 JPQL로 즉시 실행한다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PersonalActivityScore s where s.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // clearAutomatically 금지: 팀 순회 루프(TeamMemberCleanupService) 안의 TeamDeletedEvent
    // 리스너가 호출하므로, clear하면 아직 순회하지 않은 팀이 detach되어 리더 위임
    // dirty checking이 유실된다. flush는 선행 pending 반영을 위해 유지.
    @Modifying(flushAutomatically = true)
    @Query("delete from PersonalActivityScore s where s.team.id = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);

}
