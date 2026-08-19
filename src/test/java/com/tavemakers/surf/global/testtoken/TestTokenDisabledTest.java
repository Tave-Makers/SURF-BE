package com.tavemakers.surf.global.testtoken;

import com.tavemakers.surf.e2e.E2ESupport;
import com.tavemakers.surf.global.testtoken.controller.TestTokenCreateController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 안전 게이트 검증 — {@code test-token.enabled} 미설정(기본 false)이면 테스트 토큰 발급 기능이 존재하지 않는다.
 * 운영 기본값과 동일한 상태이므로, 이 테스트가 깨지면 인증 우회 백도어가 열린 것이다.
 */
class TestTokenDisabledTest extends E2ESupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("프로퍼티 미설정 시 테스트 토큰 컨트롤러 빈이 컨텍스트에 없다")
    void controllerBean_isAbsent_whenPropertyMissing() {
        assertThat(applicationContext.getBeanNamesForType(TestTokenCreateController.class)).isEmpty();
    }

    @Test
    @DisplayName("프로퍼티 미설정 시 /test/tokens 호출은 통과하지 못한다(4xx)")
    void endpoint_isNotReachable_whenPropertyMissing() throws Exception {
        mockMvc.perform(post("/test/tokens")
                        .contentType("application/json")
                        .content("{\"memberId\":1}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("게이트가 닫혔는데 토큰 발급이 성공했다 — 인증 우회 백도어")
                        .isBetween(400, 499));
    }
}
