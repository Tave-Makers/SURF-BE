package com.tavemakers.surf.global.config;

import com.tavemakers.surf.domain.auth.common.resolver.ClientTypeArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 공통 설정.
 * <p>현재는 {@link ClientTypeArgumentResolver} 등록 용도. 향후 인터셉터/CORS/MessageConverter 등 횡단 관심사가 추가되는 자리.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClientTypeArgumentResolver clientTypeArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientTypeArgumentResolver);
    }
}
