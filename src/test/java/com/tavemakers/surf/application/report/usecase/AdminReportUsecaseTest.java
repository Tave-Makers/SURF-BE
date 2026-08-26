package com.tavemakers.surf.application.report.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.comment.query.CommentGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.report.query.ReportGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.exception.InvalidReportStatusChangeException;
import com.tavemakers.surf.domain.report.repository.ReportRepository;
import com.tavemakers.surf.presentation.report.dto.request.ReportStatusPatchReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportDetailResDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportSliceResDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportPreviewResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminReportUsecaseTest {

    @Mock
    private ReportGetService reportGetService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberGetService memberGetService;
    @Mock
    private PostGetService postGetService;
    @Mock
    private CommentGetService commentGetService;

    @InjectMocks
    private AdminReportUsecase adminReportUsecase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private Member member(Long id, String name) {
        Member member = Member.builder()
                .name(name)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Report report(Long id, ReportStatus status) throws Exception {
        ReportPreviewResDTO preview = ReportPreviewResDTO.forPost("홍길동", "스팸 제목");
        Report report = Report.of(
                10L,
                20L,
                ReportTargetType.POST,
                99L,
                ReportReasonType.SPAM_OR_PROMOTION,
                objectMapper.writeValueAsString(preview)
        );
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 8, 16, 10, 0));
        return report;
    }

    private Post post(Long id, Long memberId) {
        Post post = Post.builder()
                .title("게시글 제목")
                .content("게시글 내용")
                .member(member(memberId, "피신고자"))
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    @Test
    @DisplayName("신고 목록 조회는 엔티티 Slice를 관리자 목록 DTO Slice로 매핑한다")
    void getReports_mapsSliceToAdminDto() throws Exception {
        Slice<Report> reportSlice = new SliceImpl<>(List.of(report(1L, ReportStatus.PENDING)), PageRequest.of(0, 20), false);
        given(reportGetService.getReports(null, null, PageRequest.of(0, 20))).willReturn(reportSlice);
        given(memberGetService.getMember(10L)).willReturn(member(10L, "신고자"));
        given(memberGetService.getMember(20L)).willReturn(member(20L, "피신고자"));

        AdminReportSliceResDTO result = adminReportUsecase.getReports(null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reporterName()).isEqualTo("신고자");
        assertThat(result.content().get(0).reportedName()).isEqualTo("피신고자");
        assertThat(result.content().get(0).preview().title()).isEqualTo("스팸 제목");
    }

    @Test
    @DisplayName("신고 상태 변경은 처리자와 처리 시각, 관리자 메모를 함께 저장한다")
    void updateStatus_updatesResolvedMetadata() throws Exception {
        Report resolvedReport = report(1L, ReportStatus.RESOLVED);
        ReflectionTestUtils.setField(resolvedReport, "resolvedBy", 1L);
        ReflectionTestUtils.setField(resolvedReport, "resolvedAt", LocalDateTime.of(2026, 8, 16, 13, 0));
        ReflectionTestUtils.setField(resolvedReport, "adminMemo", "스팸 확인 후 처리");

        given(reportRepository.updateStatusIfCurrentStatusMatches(
                eq(1L),
                eq(ReportStatus.PENDING),
                eq(ReportStatus.RESOLVED),
                eq(1L),
                any(LocalDateTime.class),
                eq("스팸 확인 후 처리")
        )).willReturn(1);
        given(reportGetService.getReport(1L)).willReturn(resolvedReport);
        given(memberGetService.getMember(10L)).willReturn(member(10L, "신고자"));
        given(memberGetService.getMember(20L)).willReturn(member(20L, "피신고자"));
        given(memberGetService.getMember(1L)).willReturn(member(1L, "관리자"));
        given(postGetService.getPost(99L)).willReturn(post(99L, 20L));

        AdminReportDetailResDTO result = adminReportUsecase.patchReportStatus(
                1L,
                1L,
                new ReportStatusPatchReqDTO(ReportStatus.RESOLVED, "스팸 확인 후 처리")
        );

        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.resolvedBy()).isEqualTo(1L);
        assertThat(result.resolvedByName()).isEqualTo("관리자");
        assertThat(result.adminMemo()).isEqualTo("스팸 확인 후 처리");
        assertThat(result.resolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리된 신고는 다시 상태 변경할 수 없다")
    void updateStatus_throwsWhenReportAlreadyProcessed() {
        // 이미 처리된 신고는 PENDING 조건의 CAS 업데이트가 0건으로 끝난다
        given(reportRepository.updateStatusIfCurrentStatusMatches(
                eq(1L),
                eq(ReportStatus.PENDING),
                eq(ReportStatus.REJECTED),
                eq(1L),
                any(LocalDateTime.class),
                eq("재처리 시도")
        )).willReturn(0);

        assertThatThrownBy(() -> adminReportUsecase.patchReportStatus(
                1L,
                1L,
                new ReportStatusPatchReqDTO(ReportStatus.REJECTED, "재처리 시도")
        )).isInstanceOf(InvalidReportStatusChangeException.class);
    }
}
