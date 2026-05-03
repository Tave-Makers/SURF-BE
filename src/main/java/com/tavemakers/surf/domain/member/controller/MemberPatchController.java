package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.service.MemberPatchService;
import com.tavemakers.surf.domain.member.usecase.MemberUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "회원 정보 수정", description = "회원 정보 수정 관련 API")
public class MemberPatchController {

    private final MemberUsecase memberUsecase;
    private final MemberPatchService memberPatchService;

    @Operation(
            summary = "약관 동의",
            description = "회원이 약관에 동의하는 API 입니다.")
    @PatchMapping("/v1/user/members/terms/agree")
    public ApiResponse<Void> agreeTerms() {
        Long memberId = SecurityUtils.getCurrentMemberId();
        memberPatchService.agreeTerms(memberId);
        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.TERMS_AGREEMENT_SUCCESS.getMessage(),
                null);
    }

    @Operation(
            summary = "회원 프로필 수정하기",
            description = "마이페이지에서 프로필을 수정하는 API 입니다.")
    @PatchMapping("/v1/user/members/profile/update")
    public ApiResponse<List<ProfileUpdateReqDTO>> updateProfile(
            @Valid @RequestBody ProfileUpdateReqDTO profileUpdateReqDTO
            )
    {
        Long memberId = SecurityUtils.getCurrentMemberId();
        memberUsecase.updateProfile(memberId, profileUpdateReqDTO);
        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.MYPAGE_PROFILE_UPDATE_SUCCESS.getMessage(),
                null);
    }
}
