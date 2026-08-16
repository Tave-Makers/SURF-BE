package com.tavemakers.surf.application.report.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.exception.ReportSnapshotSerializationException;
import com.tavemakers.surf.domain.report.exception.SelfReportNotAllowedException;
import com.tavemakers.surf.domain.report.service.ReportCreateService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.presentation.report.dto.request.ReportCreateReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportPreviewResDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportUsecase {

    private final ReportCreateService reportService;
    private final PostGetService postGetService;
    private final CommentGetService commentGetService;
    private final MemberGetService memberGetService;
    private final ObjectMapper objectMapper;

    @Transactional
    @LogEvent(value = "report.create", message = "신고 접수")
    public ReportResDTO createReport(Long reporterMemberId, ReportCreateReqDTO request) {
        ResolvedTarget resolvedTarget = resolveTarget(request.targetType(), request.targetId());
        validateSelfReport(reporterMemberId, resolvedTarget.reportedMemberId());

        Report report = reportService.createReport(
                reporterMemberId,
                resolvedTarget.reportedMemberId(),
                request.targetType(),
                request.targetId(),
                request.reasonType(),
                toSnapshotJson(resolvedTarget.preview())
        );

        return ReportResDTO.from(report, resolvedTarget.preview());
    }

    private ResolvedTarget resolveTarget(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> resolvePost(targetId);
            case COMMENT -> resolveComment(targetId);
            case PROFILE -> resolveProfile(targetId);
        };
    }

    private ResolvedTarget resolvePost(Long postId) {
        Post post = postGetService.getPost(postId);
        ReportPreviewResDTO preview = ReportPreviewResDTO.forPost(
                post.getMember().getName(),
                post.getTitle()
        );
        return new ResolvedTarget(post.getMember().getId(), preview);
    }

    private ResolvedTarget resolveComment(Long commentId) {
        Comment comment = commentGetService.getComment(commentId);
        ReportPreviewResDTO preview = ReportPreviewResDTO.forComment(
                comment.getMember().getName(),
                comment.getContent()
        );
        return new ResolvedTarget(comment.getMember().getId(), preview);
    }

    private ResolvedTarget resolveProfile(Long memberId) {
        Member member = memberGetService.getMember(memberId);
        ReportPreviewResDTO preview = ReportPreviewResDTO.forProfile(member.getName());
        return new ResolvedTarget(member.getId(), preview);
    }

    private String toSnapshotJson(ReportPreviewResDTO preview) {
        try {
            return objectMapper.writeValueAsString(preview);
        } catch (JsonProcessingException e) {
            throw new ReportSnapshotSerializationException();
        }
    }

    private void validateSelfReport(Long reporterMemberId, Long reportedMemberId) {
        if (reporterMemberId.equals(reportedMemberId)) {
            throw new SelfReportNotAllowedException();
        }
    }

    private record ResolvedTarget(
            Long reportedMemberId,
            ReportPreviewResDTO preview
    ) {
    }
}
