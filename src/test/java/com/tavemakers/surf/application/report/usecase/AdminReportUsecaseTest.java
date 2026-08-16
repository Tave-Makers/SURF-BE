package com.tavemakers.surf.application.report.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.report.query.ReportGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.report.entity.Report;
import com.tavemakers.surf.domain.report.entity.ReportReasonType;
import com.tavemakers.surf.domain.report.entity.ReportStatus;
import com.tavemakers.surf.domain.report.entity.ReportTargetType;
import com.tavemakers.surf.domain.report.exception.InvalidReportStatusChangeException;
import com.tavemakers.surf.presentation.report.dto.request.ReportStatusUpdateReqDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportDetailResDTO;
import com.tavemakers.surf.presentation.report.dto.response.AdminReportSliceResDTO;
import com.tavemakers.surf.presentation.report.dto.response.ReportPreviewResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminReportUsecaseTest {

    @Mock
    private ReportGetService reportGetService;
    @Mock
    private MemberGetService memberGetService;

    @InjectMocks
    private AdminReportUsecase adminReportUsecase;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        Report report = report(1L, ReportStatus.PENDING);
        given(reportGetService.getReport(1L)).willReturn(report);
        given(memberGetService.getMember(10L)).willReturn(member(10L, "신고자"));
        given(memberGetService.getMember(20L)).willReturn(member(20L, "피신고자"));
        given(memberGetService.getMember(1L)).willReturn(member(1L, "관리자"));

        AdminReportDetailResDTO result = adminReportUsecase.updateStatus(
                1L,
                1L,
                new ReportStatusUpdateReqDTO(ReportStatus.RESOLVED, "스팸 확인 후 처리")
        );

        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.resolvedBy()).isEqualTo(1L);
        assertThat(result.resolvedByName()).isEqualTo("관리자");
        assertThat(result.adminMemo()).isEqualTo("스팸 확인 후 처리");
        assertThat(result.resolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리된 신고는 다시 상태 변경할 수 없다")
    void updateStatus_throwsWhenReportAlreadyProcessed() throws Exception {
        given(reportGetService.getReport(1L)).willReturn(report(1L, ReportStatus.RESOLVED));

        assertThatThrownBy(() -> adminReportUsecase.updateStatus(
                1L,
                1L,
                new ReportStatusUpdateReqDTO(ReportStatus.REJECTED, "재처리 시도")
        )).isInstanceOf(InvalidReportStatusChangeException.class);
    }
}
