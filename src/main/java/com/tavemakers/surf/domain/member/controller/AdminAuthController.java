package com.tavemakers.surf.domain.member.controller;

import com.tavemakers.surf.domain.member.dto.request.AdminPageLoginReqDTO;
import com.tavemakers.surf.domain.member.dto.request.PasswordReqDTO;
import com.tavemakers.surf.domain.member.dto.response.AdminPageLoginResDTO;
import com.tavemakers.surf.domain.member.usecase.MemberAdminUsecase;
import io.swagger.v3.oas.annotations.Operation;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.member.controller.ResponseMessage.*;

@Tag(name = "관리자 인증", description = "관리자 인증 관련 API")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AdminAuthController {

    private final MemberAdminUsecase memberAdminUsecase;

    @Operation(summary = "비밀번호 설정", description = "관리자의 비밀번호를 설정합니다.")
    @PatchMapping("/v1/manager/password")
    public ApiResponse<Void> setUpPassword(@RequestBody PasswordReqDTO dto) {
        memberAdminUsecase.setUpPassword(dto);
        return ApiResponse.response(HttpStatus.OK, MANAGER_PASSWORD_SET_UP_SUCCESS.getMessage(),null);
    }

    @Operation(summary = "관리자 페이지 로그인", description = "관리자 페이지에 로그인합니다.")
    @PostMapping("/v1/manager/sign-in")
    public ApiResponse<AdminPageLoginResDTO> loginAdminPage(
            @RequestBody AdminPageLoginReqDTO dto,
            HttpServletResponse response
    ) {
        AdminPageLoginResDTO data = memberAdminUsecase.loginAdminHomePage(dto, response);
        return ApiResponse.response(HttpStatus.OK, ADMIN_PAGE_LOGIN_SUCCESS.getMessage(),data);
    }

}
