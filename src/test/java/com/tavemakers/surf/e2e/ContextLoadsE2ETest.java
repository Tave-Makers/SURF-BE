package com.tavemakers.surf.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 컨텍스트 로딩 스모크 테스트.
 *
 * <p>이 테스트가 통과하면 = layer-first 재편 + 전 도메인 B안 전환 이후에도 전체 빈 배선이 성립하고
 * (순환참조/컴포넌트 스캔 누락/생성자 주입 실패 없음), 모든 엔티티의 H2 스키마 생성이 성공하며,
 * SecurityFilterChain·JWT 필터·이벤트 리스너·스케줄러·설정 프로퍼티 바인딩이 모두 유효하다는 강한 증거다.
 */
class ContextLoadsE2ETest extends E2ESupport {

    @Autowired
    private WebApplicationContext context;

    @Test
    @DisplayName("전체 Spring 컨텍스트가 부팅되고 모든 빈이 배선된다")
    void contextLoads() {
        assertThat(context).isNotNull();
        // 계층별 대표 빈이 실제로 등록되어 있는지 확인 (layer-first 재편 후 스캔 누락 방지)
        assertThat(context.containsBean("securityFilterChain")).isTrue();
        assertThat(context.getBeanNamesForType(
                com.tavemakers.surf.application.post.usecase.PostCreateUsecase.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(
                com.tavemakers.surf.application.home.usecase.HomeUsecase.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(
                com.tavemakers.surf.global.jwt.JwtService.class)).isNotEmpty();
    }
}
