package com.tavemakers.surf.domain.auth.apple.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Apple OAuth 설정값 — application.yml {@code apple.*} 바인딩 */
@Getter
@ConfigurationProperties(prefix = "apple")
@Validated
public class AppleOAuthProps {

    /** Web 흐름 client_id + aud 검증용 Service ID */
    @NotBlank private final String serviceClientId;

    /** App 흐름 aud 검증용 Bundle ID (D4) */
    @NotBlank private final String appBundleId;

    @NotBlank private final String teamId;

    @NotBlank private final String keyId;

    /** Base64 단일 라인 인코딩된 P8 PrivateKey (D7) */
    @NotBlank private final String privateKey;

    /** Web 콜백 절대 URL */
    @NotBlank private final String redirectUri;

    /** Apple OAuth 설정 값을 생성한다. */
    public AppleOAuthProps(String serviceClientId, String appBundleId, String teamId,
                           String keyId, String privateKey, String redirectUri) {
        this.serviceClientId = serviceClientId;
        this.appBundleId = appBundleId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = privateKey;
        this.redirectUri = redirectUri;
    }
}
