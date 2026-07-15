package com.tavemakers.surf.global.logging;

import java.time.Duration;
import java.time.Instant;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 REST 응답에 Server-Timing 헤더를 붙여 프론트가 DevTools에서 서버 처리시간을 바로 확인하게 한다.
 * 시작 시각은 WebLoggingFilter가 기록한 RequestLogContext.startAt을 재사용한다.
 * <p>ResponseBodyAdvice 시점은 응답 커밋 전이라 헤더 추가가 항상 유효하다.
 * 본문 없는 응답(204 등)은 이 지점을 지나지 않으므로 헤더가 붙지 않는다 — 계측 용도로 허용.
 */
@RestControllerAdvice
public class ServerTimingResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        Instant startAt = RequestLogContext.get().startAt;
        if (startAt != null) {
            long durMs = Duration.between(startAt, Instant.now()).toMillis();
            response.getHeaders().add("Server-Timing", "app;dur=" + durMs);
            // 프론트가 다른 오리진이라 Timing-Allow-Origin 없이는 브라우저가 Server-Timing을 숨긴다
            response.getHeaders().add("Timing-Allow-Origin", "*");
        }
        return body;
    }
}
