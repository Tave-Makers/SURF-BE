package com.tavemakers.surf.presentation.auth.common.controller;

import com.tavemakers.surf.application.auth.common.usecase.AdminPageLoginUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.presentation.member.dto.request.AdminPageLoginReqDTO;
import com.tavemakers.surf.presentation.member.dto.response.AdminPageLoginResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 페이지 로그인 — 토큰 발급이 핵심이므로 auth 도메인에 둔다 */
@Tag(name = "관리자 인증", description = "관리자 인증 관련 API")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AdminPageLoginController {

    private static final String ADMIN_PAGE_LOGIN_SUCCESS = "[관리자 페이지]에 성공적으로 로그인했습니다.";

    private final AdminPageLoginUsecase adminPageLoginUsecase;

    /** 관리자 페이지에 로그인한다. */
    @Operation(summary = "관리자 페이지 로그인", description = "관리자 페이지에 로그인합니다.")
    @PostMapping("/v1/manager/sign-in")
    public ApiResponse<AdminPageLoginResDTO> loginAdminPage(
            @RequestBody AdminPageLoginReqDTO dto,
            HttpServletResponse response
    ) {
        AdminPageLoginResDTO data = adminPageLoginUsecase.login(dto, response);
        return ApiResponse.response(HttpStatus.OK, ADMIN_PAGE_LOGIN_SUCCESS, data);
    }
}
