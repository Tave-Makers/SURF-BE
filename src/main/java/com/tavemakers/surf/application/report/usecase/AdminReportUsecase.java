package com.tavemakers.surf.application.report.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.application.report.query.ReportGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.exception.InvalidReportStatusChangeException;
import com.tavemakers.surf.domain.report.exception.ReportSnapshotDeserializationException;
import com.tavemakers.surf.domain.report.repository.ReportRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.presentation.report.dto.request.ReportStatusPatchReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportDetailResDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportListItemResDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportSliceResDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportPreviewResDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportTargetNavigationResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportUsecase {

    private final ReportGetService reportGetService;
    private final ReportRepository reportRepository;
    private final MemberGetService memberGetService;
    private final PostGetService postGetService;
    private final CommentGetService commentGetService;
    private final ObjectMapper objectMapper;

    /** 신고 목록을 관리자 화면용으로 조회한다. */
    @Transactional(readOnly = true)
    public AdminReportSliceResDTO getReports(ReportTargetType targetType, ReportStatus status, Pageable pageable) {
        Slice<AdminReportListItemResDTO> slice = reportGetService.getReports(targetType, status, pageable)
                .map(this::toListItemDto);
        return AdminReportSliceResDTO.from(slice);
    }

    /** 신고 상세를 관리자 화면용으로 조회한다. */
    @Transactional(readOnly = true)
    public AdminReportDetailResDTO getReport(Long reportId) {
        Report report = reportGetService.getReport(reportId);
        String reporterName = memberGetService.getMember(report.getReporterMemberId()).getName();
        String reportedName = memberGetService.getMember(report.getReportedMemberId()).getName();
        String resolvedByName = resolveMemberName(report.getResolvedBy());

        return AdminReportDetailResDTO.from(
                report,
                reporterName,
                reportedName,
                resolvedByName,
                parseSnapshot(report.getSnapshotJson()),
                resolveTargetNavigation(report)
        );
    }

    /** 신고 상태를 처리 완료 또는 반려로 변경한다. */
    @Transactional
    @LogEvent(value = "report.admin.status.update", message = "관리자 신고 상태 변경")
    public AdminReportDetailResDTO patchReportStatus(Long reportId, Long adminMemberId, ReportStatusPatchReqDTO request) {
        validateStatusChange(request.status());

        int updatedCount = reportRepository.updateStatusIfCurrentStatusMatches(
                reportId,
                ReportStatus.PENDING,
                request.status(),
                adminMemberId,
                LocalDateTime.now(),
                request.adminMemo()
        );

        if (updatedCount == 0) {
            throw new InvalidReportStatusChangeException();
        }

        Report report = reportGetService.getReport(reportId);

        String reporterName = memberGetService.getMember(report.getReporterMemberId()).getName();
        String reportedName = memberGetService.getMember(report.getReportedMemberId()).getName();
        String resolvedByName = memberGetService.getMember(adminMemberId).getName();

        return AdminReportDetailResDTO.from(
                report,
                reporterName,
                reportedName,
                resolvedByName,
                parseSnapshot(report.getSnapshotJson()),
                resolveTargetNavigation(report)
        );
    }

    private AdminReportListItemResDTO toListItemDto(Report report) {
        Member reporter = memberGetService.getMember(report.getReporterMemberId());
        Member reported = memberGetService.getMember(report.getReportedMemberId());
        return AdminReportListItemResDTO.of(
                report,
                reporter.getName(),
                reported.getName(),
                parseSnapshot(report.getSnapshotJson())
        );
    }

    private ReportPreviewResDTO parseSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, ReportPreviewResDTO.class);
        } catch (JsonProcessingException e) {
            throw new ReportSnapshotDeserializationException();
        }
    }

    private String resolveMemberName(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberGetService.getMember(memberId).getName();
    }

    private ReportTargetNavigationResDTO resolveTargetNavigation(Report report) {
        return switch (report.getTargetType()) {
            case POST -> resolvePostNavigation(report.getTargetId());
            case COMMENT -> resolveCommentNavigation(report.getTargetId());
            case PROFILE -> ReportTargetNavigationResDTO.forProfile(report.getTargetId());
        };
    }

    private ReportTargetNavigationResDTO resolvePostNavigation(Long postId) {
        Post post = postGetService.getPost(postId);
        return ReportTargetNavigationResDTO.forPost(post.getId(), post.getMember().getId());
    }

    private ReportTargetNavigationResDTO resolveCommentNavigation(Long commentId) {
        Comment comment = commentGetService.getComment(commentId);
        return ReportTargetNavigationResDTO.forComment(
                comment.getPost().getId(),
                comment.getId(),
                comment.getMember().getId()
        );
    }

    private void validateStatusChange(ReportStatus nextStatus) {
        if (nextStatus != ReportStatus.RESOLVED && nextStatus != ReportStatus.REJECTED) {
            throw new InvalidReportStatusChangeException();
        }
    }
}
