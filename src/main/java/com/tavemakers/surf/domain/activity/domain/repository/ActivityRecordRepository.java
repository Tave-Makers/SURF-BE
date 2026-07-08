package com.tavemakers.surf.domain.activity.domain.repository;

import com.tavemakers.surf.domain.activity.domain.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.domain.entity.enums.ScoreType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

    /** 활동기록 행 잠금 조회 — 삭제/수정 경로를 직렬화하고 커밋된 최신 isDeleted 상태를 읽는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ar FROM ActivityRecord ar WHERE ar.id = :id")
    Optional<ActivityRecord> findByIdForUpdate(@Param("id") Long id);

    // 파생 delete(SELECT 후 개별 remove 큐잉)는 flush가 지연되어 다른 리스너의
    // clearAutomatically에 의해 취소될 수 있으므로 벌크 JPQL로 즉시 실행한다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ActivityRecord ar WHERE ar.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // clearAutomatically 금지: 팀 순회 루프(TeamMemberCleanupService) 안의 TeamDeletedEvent
    // 리스너가 호출하므로, clear하면 아직 순회하지 않은 팀이 detach되어 리더 위임
    // dirty checking이 유실된다. flush는 선행 pending 반영을 위해 유지.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ActivityRecord ar WHERE ar.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT ar " +
            "FROM ActivityRecord ar " +
            "WHERE ar.memberId = :memberId " +
            "AND ar.isDeleted = false " +
            "AND ar.scoreType = :scoreType")
    Slice<ActivityRecord> findActivityRecordListByMemberId(
            @Param("memberId") Long memberId,
            @Param("scoreType") ScoreType scoreType,
            Pageable pageable
    );

    List<ActivityRecord> findByMemberIdAndIsDeleted(Long memberId,Boolean isDeleted);

    /** 다수 회원의 상/벌점 집계 조회 */
    @Query("SELECT ar.memberId AS groupId, ar.scoreType AS scoreType, SUM(ar.appliedScore) AS totalScore " +
            "FROM ActivityRecord ar " +
            "WHERE ar.memberId IN :memberIds AND ar.isDeleted = false " +
            "GROUP BY ar.memberId, ar.scoreType")
    List<ScoreAggregation> findScoreAggregationByMemberIds(@Param("memberIds") List<Long> memberIds);

    /** 회원의 전체 활동기록 페이징 조회 (삭제되지 않은 기록만) */
    @Query("SELECT ar " +
            "FROM ActivityRecord ar " +
            "WHERE ar.memberId = :memberId " +
            "AND ar.isDeleted = false")
    Slice<ActivityRecord> findAllActiveByMemberId(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    /** 팀의 전체 활동기록 페이징 조회 (삭제되지 않은 기록만) */
    @Query("SELECT ar " +
            "FROM ActivityRecord ar " +
            "WHERE ar.teamId = :teamId " +
            "AND ar.isDeleted = false")
    Slice<ActivityRecord> findAllActiveByTeamId(
            @Param("teamId") Long teamId,
            Pageable pageable
    );

}
