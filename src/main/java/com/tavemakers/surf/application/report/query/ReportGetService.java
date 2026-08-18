package com.tavemakers.surf.application.report.query;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.exception.ReportNotFoundException;
import com.tavemakers.surf.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportGetService {

    private final ReportRepository reportRepository;

    /** 신고 목록을 조건에 맞게 최신순 조회한다. */
    @Transactional(readOnly = true)
    public Slice<Report> getReports(ReportTargetType targetType, ReportStatus status, Pageable pageable) {
        if (targetType != null && status != null) {
            return reportRepository.findByTargetTypeAndStatusOrderByCreatedAtDescIdDesc(targetType, status, pageable);
        }
        if (targetType != null) {
            return reportRepository.findByTargetTypeOrderByCreatedAtDescIdDesc(targetType, pageable);
        }
        if (status != null) {
            return reportRepository.findByStatusOrderByCreatedAtDescIdDesc(status, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
    }

    /** 신고 ID로 상세 엔티티를 조회한다. */
    @Transactional(readOnly = true)
    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);
    }
}
