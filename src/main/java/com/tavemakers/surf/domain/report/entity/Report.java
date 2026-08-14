package com.tavemakers.surf.domain.report.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(nullable = false)
    private Long reporterMemberId;

    @Column(nullable = false)
    private Long reportedMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReasonType reasonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Lob
    @Column(nullable = false)
    private String snapshotJson;

    @Builder
    private Report(
            Long reporterMemberId,
            Long reportedMemberId,
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            ReportStatus status,
            String snapshotJson
    ) {
        this.reporterMemberId = reporterMemberId;
        this.reportedMemberId = reportedMemberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonType = reasonType;
        this.status = status;
        this.snapshotJson = snapshotJson;
    }

    public static Report of(
            Long reporterMemberId,
            Long reportedMemberId,
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            String snapshotJson
    ) {
        return Report.builder()
                .reporterMemberId(reporterMemberId)
                .reportedMemberId(reportedMemberId)
                .targetType(targetType)
                .targetId(targetId)
                .reasonType(reasonType)
                .status(ReportStatus.PENDING)
                .snapshotJson(snapshotJson)
                .build();
    }
}
