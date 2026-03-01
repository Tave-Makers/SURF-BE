package com.tavemakers.surf.domain.activity.repository;

import com.tavemakers.surf.domain.activity.entity.ActivityRecord;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

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