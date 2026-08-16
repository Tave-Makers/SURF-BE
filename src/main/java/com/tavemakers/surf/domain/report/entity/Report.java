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
import java.time.LocalDateTime;
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

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(length = 1000)
    private String adminMemo;

    @Builder
    private Report(
            Long reporterMemberId,
            Long reportedMemberId,
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            ReportStatus status,
            String snapshotJson,
            Long resolvedBy,
            LocalDateTime resolvedAt,
            String adminMemo
    ) {
        this.reporterMemberId = reporterMemberId;
        this.reportedMemberId = reportedMemberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonType = reasonType;
        this.status = status;
        this.snapshotJson = snapshotJson;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.adminMemo = adminMemo;
    }

    /** 신고 엔티티 생성 */
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

    /** 신고를 처리 완료 상태로 변경한다. */
    public void resolve(Long adminMemberId, String adminMemo) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedBy = adminMemberId;
        this.resolvedAt = LocalDateTime.now();
        this.adminMemo = adminMemo;
    }

    /** 신고를 반려 상태로 변경한다. */
    public void reject(Long adminMemberId, String adminMemo) {
        this.status = ReportStatus.REJECTED;
        this.resolvedBy = adminMemberId;
        this.resolvedAt = LocalDateTime.now();
        this.adminMemo = adminMemo;
    }
}
