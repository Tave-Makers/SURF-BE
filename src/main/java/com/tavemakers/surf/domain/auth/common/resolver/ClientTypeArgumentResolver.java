package com.tavemakers.surf.domain.auth.common.resolver;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 메서드 파라미터에 {@link ClientType} 이 있으면 {@code X-Client-Type} 헤더에서 자동 주입한다.
 * <p>헤더 누락 시 {@link ClientType#WEB} fallback.
 */
@Component
public class ClientTypeArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_NAME = "X-Client-Type";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == ClientType.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = (request == null) ? null : request.getHeader(HEADER_NAME);
        return ClientType.from(header);
    }
}
