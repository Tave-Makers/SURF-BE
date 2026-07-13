package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.application.member.usecase.MemberAdminUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "관리자 회원가입 승인/거절", description = "관리자의 회원가입 승인/거절 API")
public class MemberApprovalController {

    private final MemberAdminUsecase memberAdminUsecase;

    /**
     * (관리자의) 회원가입 승인
     * event: signup.approve
     */
    @Operation(
            summary = "회원가입 승인",
            description = "관리자가 회원의 회원가입을 승인합니다."
    )
    @PatchMapping("/v1/admin/members/approve")
    public ApiResponse<Void> approveMember(
            @RequestBody List<Long> memberIds
    ) {
        Long approverId = SecurityUtils.getCurrentMemberId();
        memberAdminUsecase.approveMember(memberIds, approverId);

        return ApiResponse.response(HttpStatus.OK, "승인되었습니다.", null);
    }

    /**
     * (관리자의) 회원가입 거절
     * event: signup.reject
     */
    @Operation(
            summary = "회원가입 거절",
            description = "관리자가 회원의 회원가입을 거절합니다."
    )
    @PatchMapping("/v1/admin/members/reject")
    public ApiResponse<Void> rejectMember(
            @RequestBody List<Long> memberIds
    ) {
        Long approverId = SecurityUtils.getCurrentMemberId();
        memberAdminUsecase.rejectMember(memberIds, approverId);

        return ApiResponse.response(HttpStatus.OK, "거절되었습니다.", null);
    }
}
