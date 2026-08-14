package com.tavemakers.surf.domain.report.repository;

import com.tavemakers.surf.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
