package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.presentation.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSignupResDTO;
import com.tavemakers.surf.presentation.member.dto.response.OnboardingCheckResDTO;
import com.tavemakers.surf.application.member.usecase.MemberUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Tag(name = "자체 회원가입", description = "회원가입 및 온보딩 관련 API")
public class MemberSignupController {

    private final MemberUsecase memberUsecase;
    private final LogEventEmitter logEventEmitter;

    /**
     * 1) 자체 회원가입 온보딩
     * event: signup.create / signup.succeeded & signup.failed
     */
    @Operation(
            summary = "자체 회원가입 온보딩",
            description = "카카오 로그인 후 추가 정보를 입력하여 회원가입을 요청합니다."
    )
    @PostMapping("/v1/user/members/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberSignupResDTO> signup(
            @Valid @RequestBody MemberSignupReqDTO request,
            @Parameter(hidden = true) @LogParam("request_id") String requestId
    ) {
        Long userId = SecurityUtils.getCurrentMemberId();

        // 실패 예외는 GlobalExceptionHandler 로 흘려보내 실제 HTTP 상태(403/409/400)로 응답한다.
        // 컨트롤러에서 잡아 ApiResponse 로 감싸면 상태가 200 으로 고정되므로 여기서 catch 하지 않는다.
        return ApiResponse.response(
                HttpStatus.CREATED,
                "회원가입 요청 접수",
                memberUsecase.signup(userId, request)
        );
    }

    /**
     * 2) 온보딩(추가 정보 입력) 필요 여부 확인
     * event: onboarding.valid_status
     */
    @Operation(
            summary = "온보딩(추가 정보 입력) 필요 여부 확인",
            description = "카카오 ID로 회원을 조회하여 추가 정보 입력이 필요한 상태인지 확인합니다."
    )
    @GetMapping("/v1/user/members/valid-status")
    public ApiResponse<OnboardingCheckResDTO> checkOnboardingStatus() {
        Long userId = SecurityUtils.getCurrentMemberId();
        OnboardingCheckResDTO dto = memberUsecase.needsOnboarding(userId);

        logEventEmitter.emit("onboarding.valid_status", dto.buildProps());

        return ApiResponse.response(
                HttpStatus.OK,
                ResponseMessage.MEMBER_ONBOARDING_STATUS_CHECK_SUCCESS.getMessage(),
                dto
        );
    }
}
