package com.tavemakers.surf.application.auth.common.usecase;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.service.RefreshTokenService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.AdminPageRoleException;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.presentation.member.dto.request.AdminPageLoginReqDTO;
import com.tavemakers.surf.presentation.member.dto.response.AdminPageLoginResDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 관리자 페이지 로그인 Usecase — 토큰 발급(access + refresh)은 auth 도메인 책임이므로
 * member가 아닌 auth 계층에 둔다 (R2: 타 도메인 비-Get 서비스 호출 금지).
 */
@Service
@RequiredArgsConstructor
public class AdminPageLoginUsecase {

    private final MemberGetService memberGetService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /** 관리자 페이지 로그인 처리 */
    public AdminPageLoginResDTO login(AdminPageLoginReqDTO dto, HttpServletResponse response) {
        // 관리자 페이지는 MANAGER 이상만 접근 가능하므로, 이메일로 회원 조회 후 권한 검증
        // 회원 조회 후, 권한 검증, 비밀번호 검증 순으로 진행하여 불필요한 DB 조회 방지
        Member manager = memberGetService.getMemberByEmail(dto.email());
        validateLoginMemberRole(manager);
        manager.checkPassword(dto.password());

        String accessToken = jwtService.createAccessToken(manager.getId(), manager.getRole().name());
        String deviceId = UUID.randomUUID().toString();
        response.addHeader("Set-Cookie", refreshTokenService.issue(manager.getId(), deviceId).toString());

        return AdminPageLoginResDTO.of(accessToken, manager);
    }

    private void validateLoginMemberRole(Member member) {
        if(member.isMember()){
            throw new AdminPageRoleException();
        }
    }
}
