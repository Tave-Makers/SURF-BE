package com.tavemakers.surf.domain.report.repository;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Slice<Report> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Slice<Report> findByTargetTypeOrderByCreatedAtDescIdDesc(ReportTargetType targetType, Pageable pageable);
    Slice<Report> findByStatusOrderByCreatedAtDescIdDesc(ReportStatus status, Pageable pageable);
    Slice<Report> findByTargetTypeAndStatusOrderByCreatedAtDescIdDesc(ReportTargetType targetType, ReportStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Report r
               set r.status = :nextStatus,
                   r.resolvedBy = :adminMemberId,
                   r.resolvedAt = :resolvedAt,
                   r.adminMemo = :adminMemo
             where r.id = :reportId
               and r.status = :currentStatus
            """)
    int updateStatusIfCurrentStatusMatches(
            @Param("reportId") Long reportId,
            @Param("currentStatus") ReportStatus currentStatus,
            @Param("nextStatus") ReportStatus nextStatus,
            @Param("adminMemberId") Long adminMemberId,
            @Param("resolvedAt") LocalDateTime resolvedAt,
            @Param("adminMemo") String adminMemo
    );
}
