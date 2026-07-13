package com.tavemakers.surf.e2e;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증/인가 경로 E2E — SecurityFilterChain + JwtAuthenticationFilter + @Value 기반 role 매핑을 관통한다.
 * 관통 계층: presentation(SecurityConfig) → global.jwt(filter) → domain.member(권한 로딩).
 */
class SecurityE2ETest extends E2ESupport {

    @Test
    @DisplayName("유효하지 않은 액세스 토큰으로 보호 엔드포인트 호출 시 401")
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/user/home")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 보호 엔드포인트 호출 시 인증 실패(4xx)")
    void noToken_isRejected() throws Exception {
        mockMvc.perform(get("/v1/user/home"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("MEMBER 권한으로 관리자 전용 엔드포인트 호출 시 403")
    void memberRole_onAdminEndpoint_returns403() throws Exception {
        Member member = persistMember(MemberRole.MEMBER);

        mockMvc.perform(get("/v1/admin/boards/1")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 권한은 관리자 엔드포인트 인가를 통과한다(403 이 아님)")
    void adminRole_passesAuthorization() throws Exception {
        Member admin = persistMember(MemberRole.ADMIN);

        // 인가는 통과해야 하므로 403(Forbidden)이 아니어야 한다.
        // (자원 boardId=999999 가 없어 도메인 예외로 4xx 가 날 수 있으나 그것은 인가 통과 이후의 문제다.)
        mockMvc.perform(get("/v1/admin/boards/999999")
                        .header("Authorization", bearer(admin)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 403) {
                        throw new AssertionError("ADMIN 인데 403 이 발생했다 — 인가 배선 오류");
                    }
                });
    }
}
