package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.application.member.query.IntegrationTargetGetService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import com.tavemakers.surf.presentation.member.dto.response.IntegrationTargetResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "계정 통합", description = "소셜 계정 통합(연동) API")
public class SocialAccountGetController {

    private final IntegrationTargetGetService integrationTargetGetService;

    /** 임시 회원의 계정 통합 대상을 조회한다. */
    @Operation(
            summary = "계정 통합 대상 확인",
            description = "온보딩 case B 직후, 통합될 기존 계정의 정보와 로그인해야 할 소셜(provider)을 조회합니다. "
                    + "대기 건을 소비하지 않는 읽기 전용 API 입니다."
    )
    @GetMapping("/v1/user/social-accounts/integrate/target")
    public ApiResponse<IntegrationTargetResDTO> getIntegrationTarget() {
        Long tempMemberId = SecurityUtils.getCurrentMemberId();
        IntegrationTargetResDTO data = integrationTargetGetService.getIntegrationTarget(tempMemberId);

        return ApiResponse.response(HttpStatus.OK, "통합 대상 조회 완료", data);
    }
}
