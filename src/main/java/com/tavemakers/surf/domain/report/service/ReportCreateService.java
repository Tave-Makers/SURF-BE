package com.tavemakers.surf.domain.report.service;

import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportCreateService {

    private final ReportRepository reportRepository;

    public Report createReport(
            Long reporterMemberId,
            Long reportedMemberId,
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            String snapshotJson
    ) {
        Report report = Report.of(
                reporterMemberId,
                reportedMemberId,
                targetType,
                targetId,
                reasonType,
                snapshotJson
        );
        return reportRepository.save(report);
    }
}
