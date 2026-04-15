package com.tavemakers.surf.domain.auth.kakao.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "kakao")
@Validated
public class KakaoOAuthProps {
    @NotBlank private final String clientId;
    private final String clientSecret; // optional
    @NotBlank private final String redirectUri;

    /** 카카오 OAuth 설정 값을 생성한다. */
    public KakaoOAuthProps(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
