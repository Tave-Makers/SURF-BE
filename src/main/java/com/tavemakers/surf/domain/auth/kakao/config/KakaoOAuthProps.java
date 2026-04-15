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

    /**
     * Creates a KakaoOAuthProps with the specified OAuth client configuration.
     *
     * @param clientId     the OAuth client identifier; must be non-blank when bound from configuration
     * @param clientSecret the OAuth client secret; may be null or blank if not used
     * @param redirectUri  the redirect URI to receive the authorization response; must be non-blank when bound from configuration
     */
    public KakaoOAuthProps(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
