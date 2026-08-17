package com.tavemakers.surf.global.common.moderation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 금칙어 마스킹 설정. */
@Getter
@ConfigurationProperties(prefix = "moderation")
public class ModerationProperties {

    private final boolean enabled;
    private final String maskChar;

    public ModerationProperties(Boolean enabled, String maskChar) {
        this.enabled = enabled == null || enabled;
        this.maskChar = (maskChar == null || maskChar.isEmpty()) ? "*" : maskChar;
    }
}
