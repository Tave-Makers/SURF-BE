package com.tavemakers.surf.domain.login.controller;

import com.tavemakers.surf.domain.login.facade.LoginFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인/로그아웃")
@RestController
@RequiredArgsConstructor
public class LogoutController {

    private final LoginFacade loginFacade;

    @Operation(summary = "로그아웃", description = "현재 디바이스의 refreshToken을 무효화하고 쿠키를 삭제합니다.")
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        loginFacade.logout(request, response);
        return ApiResponse.response(HttpStatus.NO_CONTENT, "로그아웃 완료", null);
    }
}