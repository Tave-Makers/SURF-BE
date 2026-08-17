package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.presentation.member.dto.request.AdminPageLoginReqDTO;
import com.tavemakers.surf.presentation.member.dto.request.PasswordReqDTO;
import com.tavemakers.surf.presentation.member.dto.request.RoleChangeReqDTOV2;
import com.tavemakers.surf.presentation.member.dto.response.*;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.AdminPageRoleException;
import com.tavemakers.surf.application.member.query.CareerGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.member.query.TrackGetService;
import com.tavemakers.surf.domain.member.service.MemberPatchService;
import com.tavemakers.surf.domain.member.service.MemberGenerationSyncService;
import com.tavemakers.surf.domain.member.service.MemberBlacklistCreateService;
import com.tavemakers.surf.domain.member.service.MemberDismissService;
import com.tavemakers.surf.domain.member.service.TrackService;
import com.tavemakers.surf.domain.member.service.MemberWithdrawService;
import com.tavemakers.surf.domain.member.validator.RoleChangeValidator;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.application.score.query.PersonalScoreGetService;
import com.tavemakers.surf.domain.score.service.PersonalScoreCreateService;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAdminUsecase {

    //<editor-fold desc="MemberAdminUsecase Dependency Summary">
    private final MemberPatchService memberPatchService;
    private final MemberGetService memberGetService;
    private final ActiveGenerationGetService activeGenerationGetService;
    private final MemberGenerationSyncService memberGenerationSyncService;
    private final MemberBlacklistCreateService memberBlacklistCreateService;
    private final MemberDismissService memberDismissService;
    private final MemberDismissUsecase memberDismissUsecase;
    private final CareerGetService careerGetService;
    private final PersonalScoreCreateService personalScoreCreateService;
    private final PersonalScoreGetService scoreGetService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TrackGetService trackGetService;
    private final TrackService trackService;
    private final MemberWithdrawService memberWithdrawService;
    private final LogEventEmitter logEventEmitter;
    private final RoleChangeValidator roleChangeValidator;
    //</editor-fold>

    /** 회원 권한 변경 */
    @Transactional
    @LogEvent(value = "role.grant", message = "회원 권한 변경")
    public void changeRole (
            @LogParam("member_id") Long memberId,
            @LogParam("role") MemberRole role
    ) {
        Member actor = SecurityUtils.getCurrentMember();
        Member member = memberGetService.getMember(memberId);
        roleChangeValidator.validate(actor, member, role);
        memberPatchService.grantRole(member, role);
    }

    /** 회원 권한 변경 Version 2 (한 번에 여러명 변경) */
    @Transactional
    @LogEvent(value = "role.bulk.grant", message = "회원 다중 권한 변경")
    public void changeMembersRole(
            @LogParam("member_ids") List<Long> memberIds,
            @LogParam("role") MemberRole role
    ) {
        Member actor = SecurityUtils.getCurrentMember();
        List<Member> members = memberGetService.findMembersByIds(memberIds);
        members.forEach(member -> roleChangeValidator.validate(actor, member, role));
        memberPatchService.grantRoleV2(members, role);
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
        Integer activeGeneration = activeGenerationGetService.getActiveGeneration();
        members.forEach(member -> memberGenerationSyncService.syncApprovedMember(member, activeGeneration));
        personalScoreCreateService.savePersonalScores(members);

        for (Member member : members) {
            logEventEmitter.emit(
                    "signup.succeeded",
                    Map.of(
                            "member_id", member.getId(),
                            "approver_id", approverId
                    )
            );
        }
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

        for (Member member : members) {
            logEventEmitter.emit(
                    "signup.failed",
                    Map.of(
                            "member_id", member.getId(),
                            "approver_id", approverId
                    ),
                    "회원가입 거절"
            );
        }
    }

    /** 회원 제명 처리 */
    @Transactional
    @LogEvent(value = "member.dismiss", message = "회원 제명 처리")
    public void dismissMember(
            @LogParam("member_id") Long memberId,
            @LogParam("actor_id") Long actorId
    ) {
        Member member = memberGetService.getMember(memberId);
        memberDismissUsecase.dismiss(member, actorId);
    }

    /** 회원 퇴출 처리 */
    @Transactional
    @LogEvent(value = "member.expel", message = "회원 퇴출 처리")
    public void expelMember(
            @LogParam("member_id") Long memberId,
            @LogParam("actor_id") Long actorId
    ) {
        Member member = memberGetService.getMember(memberId);
        memberDismissService.validateDismissible(member);
        memberBlacklistCreateService.createIfAbsent(member, MemberBlacklistActionType.EXPEL, actorId);
        memberWithdrawService.expel(member);
    }

    /** 관리자 비밀번호 설정 */
    @Transactional
    public void setUpPassword(PasswordReqDTO dto) {
        Member member = memberGetService.getMember(SecurityUtils.getCurrentMemberId());
        member.updatePassword(dto.password());
    }

    /** 관리자 페이지 로그인 처리 */
    public AdminPageLoginResDTO loginAdminHomePage(AdminPageLoginReqDTO dto, HttpServletResponse response) {
        // 관리자 페이지는 MANAGER 이상만 접근 가능하므로, 이메일로 회원 조회 후 권한 검증
        // 회원 조회 후, 권한 검증, 비밀번호 검증 순으로 진행하여 불필요한 DB 조회 방지
        Member manager = memberGetService.getMemberByEmail(dto.email());
        validateLoginMemberRole(manager);
        manager.checkPassword(dto.password());

        String accessToken = jwtService.createAccessToken(manager.getId(), manager.getRole().name());
        String deviceId = UUID.randomUUID().toString();
        response.addHeader("Set-Cookie", refreshTokenService.issue(manager.getId(), deviceId).toString());

        return AdminPageLoginResDTO.of(accessToken, manager);
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

    /** 승인된 회원 목록 스크롤 조회 */
    public ApprovedMemberSliceResDTO readApprovedMemberList(Integer generation, String keyword, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());
        Slice<MemberRegistrationDetailResDTO> approvedMemberSlice = memberGetService.getApprovedMemberList(generation, keyword, pageable)
                .map(MemberRegistrationDetailResDTO::from);
        return ApprovedMemberSliceResDTO.from(approvedMemberSlice);
    }

    @Transactional
    public void addTrack(Long memberId, Integer generation, com.tavemakers.surf.domain.member.entity.enums.Part part) {
        Member member = memberGetService.getMember(memberId);
        boolean wasActive = member.isActive();
        trackService.addTrackToMember(memberId, generation, part);
        syncApprovedMemberTrackChange(member, wasActive, generation);
    }

    /** 관리자 권한으로 특정 회원의 트랙을 추가한다. */
    @Transactional
    public void updateTrack(Long trackId, Integer generation, com.tavemakers.surf.domain.member.entity.enums.Part part) {
        Member member = trackGetService.findTrackById(trackId).getMember();
        boolean wasActive = member.isActive();
        trackService.updateTrack(trackId, generation, part);
        syncApprovedMemberTrackChange(member, wasActive, generation);
    }

    /** 관리자 권한으로 특정 트랙의 기수/파트를 수정한다. */
    @Transactional
    public void deleteTrack(Long trackId) {
        Member member = trackService.deleteTrack(trackId);
        syncApprovedMemberTrackChange(member, member.isActive(), null);
    }

    /** 관리자 권한으로 특정 트랙을 삭제한다. */
    private void validateLoginMemberRole(Member member) {
        if(member.isMember()){
            throw new AdminPageRoleException();
        }
    }

    private void syncApprovedMemberTrackChange(Member member, boolean wasActive, Integer changedGeneration) {
        if (!member.isApproved()) {
            return;
        }

        Integer activeGeneration = activeGenerationGetService.getActiveGeneration();
        memberGenerationSyncService.syncApprovedMember(member, activeGeneration);

        if (shouldResetScore(wasActive, member, activeGeneration, changedGeneration)) {
            personalScoreCreateService.resetPersonalScores(List.of(member));
        }
    }

    private boolean shouldResetScore(boolean wasActive, Member member, Integer activeGeneration, Integer changedGeneration) {
        return !wasActive
                && member.isActive()
                && changedGeneration != null
                && activeGeneration.equals(changedGeneration);
    }
}
