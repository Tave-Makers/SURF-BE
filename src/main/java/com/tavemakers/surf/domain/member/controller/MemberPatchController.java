package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.facade.MemberFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "회원 정보 수정", description = "회원 정보 수정 관련 API")
public class MemberPatchController {

    private final MemberFacade memberFacade;

    @Operation(
            summary = "회원 프로필 수정하기",
            description = "마이페이지에서 프로필을 수정하는 API 입니다.")
    @PatchMapping("/v1/user/members/profile/update")
    public ApiResponse<List<ProfileUpdateReqDTO>> updateProfile(
            @Valid @RequestBody ProfileUpdateReqDTO profileUpdateReqDTO
            )
    {
        Long memberId = SecurityUtils.getCurrentMemberId();
        memberFacade.updateProfile(memberId, profileUpdateReqDTO);
        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.MYPAGE_PROFILE_UPDATE_SUCCESS.getMessage(),
                null);
    }
}
