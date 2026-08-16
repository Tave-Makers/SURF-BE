package com.tavemakers.surf.domain.report.repository;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Slice<Report> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Slice<Report> findByTargetTypeOrderByCreatedAtDescIdDesc(ReportTargetType targetType, Pageable pageable);
    Slice<Report> findByStatusOrderByCreatedAtDescIdDesc(ReportStatus status, Pageable pageable);
    Slice<Report> findByTargetTypeAndStatusOrderByCreatedAtDescIdDesc(ReportTargetType targetType, ReportStatus status, Pageable pageable);
}
