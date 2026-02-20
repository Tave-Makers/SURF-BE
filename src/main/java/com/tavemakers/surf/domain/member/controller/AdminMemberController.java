package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.member.dto.request.MemberBanReqDTO;
import com.tavemakers.surf.domain.member.dto.request.RoleChangeReqDTO;
import com.tavemakers.surf.domain.member.dto.request.RoleChangeReqDTOV2;
import com.tavemakers.surf.domain.member.dto.response.GenerationInfoListResDTO;
import com.tavemakers.surf.domain.member.dto.response.ApprovedMemberSliceResDTO;
import com.tavemakers.surf.domain.member.dto.response.MemberInformationResDTO;
import com.tavemakers.surf.domain.member.dto.response.MemberRegistrationSliceResDTO;
import com.tavemakers.surf.domain.member.usecase.MemberAdminUsecase;
import io.swagger.v3.oas.annotations.Operation;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.member.controller.ResponseMessage.*;

@Tag(name = "관리자 회원관리", description = "관리자용 회원 관리 API")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberAdminUsecase memberAdminUsecase;

    @Operation(summary = "회원 역할 변경", description = "특정 회원의 역할을 변경합니다.")
    @PatchMapping("/v1/admin/members/{memberId}/role")
    public ApiResponse<Void> changeMemberRole(
            @PathVariable Long memberId,
            @RequestBody @Valid RoleChangeReqDTO request) {

        memberAdminUsecase.changeRole(memberId, request.role());
        return ApiResponse.response(HttpStatus.OK, "회원 역할이 성공적으로 변경되었습니다.",null);
    }

    @Operation(summary = "회원 역할 변경", description = "여러 회원들의 역할을 변경합니다.")
    @PatchMapping("/v1/admin/members/role")
    public ApiResponse<Void> changeMembersRole(
            @RequestBody RoleChangeReqDTOV2 dto
    ) {
        memberAdminUsecase.changeMembersRole(dto);
        return ApiResponse.response(HttpStatus.OK, "회원 역할이 성공적으로 변경되었습니다.",null);
    }

    @Operation(summary = "가입신청 목록", description = "가입신청 목록을 조회합니다.")
    @GetMapping("/v1/manager/registration-list")
    public ApiResponse<MemberRegistrationSliceResDTO> readRegistrationList (
            @RequestParam(required = false) String keyword,
            @RequestParam int pageSize,
            @RequestParam int pageNum
    ) {
        MemberRegistrationSliceResDTO data = memberAdminUsecase.readRegistrationList(keyword, pageSize, pageNum);
        return ApiResponse.response(HttpStatus.OK, REGISTRATION_LIST_READ.getMessage(), data);
    }

    @Operation(summary = "유저 정보 상세 조회", description = "관리자 페이지에서 유저 정보를 상세 조회합니다.")
    @GetMapping("/v1/manager/member/{memberId}")
    public ApiResponse<MemberInformationResDTO> readMemberInformation(
            @PathVariable Long memberId
    ) {
        MemberInformationResDTO data = memberAdminUsecase.readMemberInformation(memberId);
        return ApiResponse.response(HttpStatus.OK, MEMBER_INFORMATION_READ.getMessage(), data);
    }

    @Operation(summary = "존재하는 회원들의 [모든 기수 정보] 조회", description = "존재하는 회원들의 [모든 기수 정보] 조회")
    @GetMapping("/v1/manager/generations")
    public ApiResponse<GenerationInfoListResDTO> readAllMemberCountAndGeneration() {
        GenerationInfoListResDTO data = memberAdminUsecase.readExistingGenerations();
        return ApiResponse.response(HttpStatus.OK, APPROVED_ALL_GENERATION.getMessage(), data);
    }

    @Operation(summary = "승인된 [전체 회원 목록] 조회", description = "APPROVED 상태의 전체 회원 목록을 스크롤 조회")
    @GetMapping("/v1/manager/approved-members")
    public ApiResponse<ApprovedMemberSliceResDTO> readApprovedMemberList(
            @RequestParam int generation,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int pageNum
    ) {
        ApprovedMemberSliceResDTO data = memberAdminUsecase.readApprovedMemberList(generation, keyword, pageSize, pageNum);
        return ApiResponse.response(HttpStatus.OK, APPROVED_MEMBER_LIST.getMessage(), data);
    }

    @Operation(summary = "회원 퇴출/제명", description = "특정 회원들을 퇴출/제명 처리합니다. (APPROVED 상태인 member만 가능)")
    @PostMapping("/v1/manager/members/ban")
    public ApiResponse<Void> banMember(
            @RequestBody @Valid MemberBanReqDTO req
    ) {
        memberAdminUsecase.banMembers(req.memberIds());
        return ApiResponse.response(HttpStatus.OK, MEMBER_BAN_SUCCESS.getMessage(), null);
    }

    @Operation(summary = "회원 퇴출/제명 해제", description = "특정 회원들의 제명 처리를 해제합니다.")
    @PostMapping("/v1/manager/members/unban")
    public ApiResponse<Void> unbanMembers(
            @RequestBody @Valid MemberBanReqDTO req
    ) {
        memberAdminUsecase.unbanMembers(req.memberIds());
        return ApiResponse.response(HttpStatus.OK, MEMBER_UNBAN_SUCCESS.getMessage(), null);
    }

}
