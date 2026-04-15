package com.tavemakers.surf.domain.auth.kakao.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Configure and return a RestTemplate for Kakao API calls.
     *
     * <p>The returned RestTemplate has a connection timeout of 2 seconds and a read timeout of 5 seconds.
     *
     * @return a RestTemplate configured with a 2-second connection timeout and a 5-second read timeout
     */
    @Bean
    public RestTemplate kakaoRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
