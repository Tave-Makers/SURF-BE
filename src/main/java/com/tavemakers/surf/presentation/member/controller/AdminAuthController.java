package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.presentation.member.dto.request.PasswordReqDTO;
import com.tavemakers.surf.application.member.usecase.MemberAdminUsecase;
import io.swagger.v3.oas.annotations.Operation;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.member.controller.ResponseMessage.*;

@Tag(name = "관리자 인증", description = "관리자 인증 관련 API")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AdminAuthController {

    private final MemberAdminUsecase memberAdminUsecase;

    /** 관리자 비밀번호를 설정한다. */
    @Operation(summary = "비밀번호 설정", description = "관리자의 비밀번호를 설정합니다.")
    @PatchMapping("/v1/manager/password")
    public ApiResponse<Void> setUpPassword(@RequestBody PasswordReqDTO dto) {
        memberAdminUsecase.setUpPassword(dto);
        return ApiResponse.response(HttpStatus.OK, MANAGER_PASSWORD_SET_UP_SUCCESS.getMessage(),null);
    }

}
