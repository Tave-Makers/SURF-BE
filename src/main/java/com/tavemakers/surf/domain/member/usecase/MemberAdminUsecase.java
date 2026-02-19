package com.tavemakers.surf.domain.member.usecase;

import com.tavemakers.surf.domain.login.auth.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.dto.request.AdminPageLoginReqDto;
import com.tavemakers.surf.domain.member.dto.request.PasswordReqDto;
import com.tavemakers.surf.domain.member.dto.request.RoleChangeReqDTOV2;
import com.tavemakers.surf.domain.member.dto.response.*;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.AdminPageRoleException;
import com.tavemakers.surf.domain.member.service.*;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.service.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAdminUsecase {

    //<editor-fold desc="MemberAdminUsecase Dependency Summary">
    private final MemberPatchService memberPatchService;
    private final MemberGetService memberGetService;
    private final CareerGetService careerGetService;
    private final PersonalScoreCreateService personalScoreCreateService;
    private final PersonalScoreGetService scoreGetService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TrackGetService trackGetService;
    //</editor-fold>

    /** 회원 권한 변경 */
    @Transactional
    public void changeRole (Long memberId, MemberRole role) {
        Member member = memberGetService.getMember(memberId);
        memberPatchService.grantRole(member, role);
    }

    /** 회원 권한 변경 Version 2 */
    @Transactional
    public void changeMembersRole(RoleChangeReqDTOV2 dto) {
        List<Member> members = memberGetService.findMembersByIds(dto.memberIdList());
        memberPatchService.grantRoleV2(members, dto.role());
    }

    /** 회원가입 승인 처리 */
    @Transactional
    @LogEvent(value = "signup.approve", message = "회원가입 승인 처리")
    public void approveMember(
            @LogParam("member_ids") List<Long> memberIds,
            @LogParam("approver_id") Long approverId
    ) {
        List<Member> members = memberGetService.getMembersByStatus(memberIds, MemberStatus.WAITING);
        members.forEach(Member::approve);
        personalScoreCreateService.savePersonalScores(members);
    }

    /** 회원가입 거절 처리 */
    @Transactional
    @LogEvent(value = "signup.reject", message = "회원가입 거절 처리")
    public void rejectMember(
            @LogParam("member_ids") List<Long> memberIds,
            @LogParam("approver_id") Long approverId
    ) {
        List<Member> members = memberGetService.getMembersByStatus(memberIds, MemberStatus.WAITING);
        members.forEach(Member::reject);
    }

    /** 관리자 비밀번호 설정 */
    @Transactional
    public void setUpPassword(PasswordReqDto dto) {
        Member member = memberGetService.getMember(SecurityUtils.getCurrentMemberId());
        member.updatePassword(dto.password());
    }

    /** 관리자 페이지 로그인 처리 */
    public AdminPageLoginResDto loginAdminHomePage(AdminPageLoginReqDto dto, HttpServletResponse response) {
        Member member = memberGetService.getMemberByEmail(dto.email());
        member.checkPassword(dto.password());
        //validateLoginMemberRole(member);

        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());
        String deviceId = UUID.randomUUID().toString();
        refreshTokenService.issue(response, member.getId(), deviceId);

        return AdminPageLoginResDto.of(accessToken, member);
    }

    /** 가입 대기 회원 목록 조회 */
    public MemberRegistrationSliceResDTO readRegistrationList(String keyword, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());
        List<MemberStatus> statuses = List.of(MemberStatus.WAITING, MemberStatus.REJECTED);
        Slice<MemberRegistrationDetailResDTO> registrationList = memberGetService.searchWaitingMembers(keyword, pageable, statuses)
                .map(MemberRegistrationDetailResDTO::from);
        return MemberRegistrationSliceResDTO.from(registrationList);
    }

    /** 회원 상세 정보 조회 */
    public MemberInformationResDTO readMemberInformation(Long memberId) {
        Member member = memberGetService.readMemberInformation(memberId);

        List<TrackResDTO> memberTracks = member.getTracks()
                .stream()
                .map(TrackResDTO::from)
                .toList();

        List<CareerResDTO> memberCareers = careerGetService.getMemberCareers(memberId);

        if (member.isApproved()) {
            PersonalActivityScore personalScore = scoreGetService.getPersonalScore(memberId);
            return MemberInformationResDTO.of(member, memberTracks, personalScore.getScore(), memberCareers);
        }

        return MemberInformationResDTO.of(member, memberTracks, null, memberCareers);
    }

    /** 존재하는 모든 기수를 구함. */
    public GenerationInfoListResDTO readExistingGenerations() {
        List<Integer> existsAllGenerations = trackGetService.getExistsAllGenerations();
        return GenerationInfoListResDTO.from(existsAllGenerations);
    }

    /** 승인된 회원 목록 스크롤 조회 */
    public ApprovedMemberSliceResDTO readApprovedMemberList(Integer generation, String keyword, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());
        Slice<MemberRegistrationDetailResDTO> approvedMemberSlice = memberGetService.getApprovedMemberList(generation, keyword, pageable)
                .map(MemberRegistrationDetailResDTO::from);
        return ApprovedMemberSliceResDTO.from(approvedMemberSlice);
    }

    @Transactional
    public void banMembers(List<Long> memberIds) {
        memberPatchService.banMembers(memberIds);
    }

    private void validateLoginMemberRole(Member member) {
        if(member.isMember()){
            throw new AdminPageRoleException();
        }
    }
}
