package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.application.member.usecase.SocialAccountIntegrateUsecase;
import com.tavemakers.surf.presentation.member.dto.request.SocialAccountIntegrateReqDTO;
import com.tavemakers.surf.presentation.member.dto.response.SocialAccountIntegrateResDTO;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "계정 통합", description = "소셜 계정 통합(연동) API")
public class SocialAccountIntegrateController {

    private final SocialAccountIntegrateUsecase socialAccountIntegrateUsecase;

    /** 소셜 계정 통합 — 인증 주체(기존 계정 JWT)를 대상 회원으로 간주한다 (§3.6.2 c). */
    @Operation(
            summary = "소셜 계정 통합",
            description = "온보딩 case B에서 발급받은 integrationToken으로, 기존 계정 로그인 권한에서 신규 소셜 계정을 기존 회원에 연결합니다."
    )
    @PostMapping("/v1/user/social-accounts/integrate")
    public ApiResponse<SocialAccountIntegrateResDTO> integrate(
            @Valid @RequestBody SocialAccountIntegrateReqDTO request
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        SocialAccountIntegrateResDTO data =
                socialAccountIntegrateUsecase.integrate(memberId, request.integrationToken());

        return ApiResponse.response(HttpStatus.OK, "계정 통합 완료", data);
    }
}
