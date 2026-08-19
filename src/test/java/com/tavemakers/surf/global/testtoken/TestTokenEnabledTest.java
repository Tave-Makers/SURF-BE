package com.tavemakers.surf.global.testtoken;

import com.fasterxml.jackson.databind.JsonNode;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.e2e.E2ESupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게이트 활성 시 동작 검증 — 실존 회원의 DB 역할로 액세스 토큰이 발급되고,
 * 그 토큰이 실제 JwtAuthenticationFilter + 인가를 통과하는지까지 관통한다.
 */
@TestPropertySource(properties = "test-token.enabled=true")
class TestTokenEnabledTest extends E2ESupport {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("실존 회원으로 토큰이 발급되고 sub/role 클레임이 DB 값과 일치한다")
    void issuesToken_withDbRoleClaims() throws Exception {
        Member member = persistMember(MemberRole.MANAGER);

        String accessToken = requestTestToken(member.getId());

        Claims claims = parseClaims(accessToken);
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(member.getId()));
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_MANAGER");
    }

    @Test
    @DisplayName("발급된 토큰으로 보호 엔드포인트 인증을 통과한다")
    void issuedToken_passesAuthenticationFilter() throws Exception {
        Member member = persistMember(MemberRole.MEMBER);

        String accessToken = requestTestToken(member.getId());

        mockMvc.perform(get("/v1/user/home")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404")
    void unknownMember_returns404() throws Exception {
        mockMvc.perform(post("/test/tokens")
                        .contentType("application/json")
                        .content("{\"memberId\":99999999}"))
                .andExpect(status().isNotFound());
    }

    /** /test/tokens 를 호출해 액세스 토큰 문자열을 꺼낸다. */
    private String requestTestToken(Long memberId) throws Exception {
        String body = mockMvc.perform(post("/test/tokens")
                        .contentType("application/json")
                        .content("{\"memberId\":" + memberId + "}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.path("data").path("accessToken").asText();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
