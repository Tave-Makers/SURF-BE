package com.tavemakers.surf.domain.member.presentation.controller;

import com.tavemakers.surf.domain.member.application.usecase.MemberUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 탈퇴", description = "회원 탈퇴 API")
@RestController
@RequiredArgsConstructor
public class MemberWithdrawController {

    private final MemberUsecase memberUsecase;
    private final JwtService jwtService;

    @PostMapping("/v1/user/members/withdraw")
    public ApiResponse<Void> withdraw(HttpServletResponse response) {
        Long memberId = SecurityUtils.getCurrentMemberId();

        memberUsecase.withdraw(memberId);

        jwtService.clearRefreshTokenCookie(response);

        return ApiResponse.response(HttpStatus.NO_CONTENT, "회원 탈퇴 완료", null);
    }
}