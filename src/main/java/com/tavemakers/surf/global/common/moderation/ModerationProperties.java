package com.tavemakers.surf.global.common.moderation;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 금칙어 마스킹 설정. */
@Getter
@ConfigurationProperties(prefix = "moderation")
public class ModerationProperties {

    private final boolean enabled;
    private final String maskChar;

    /** 설정을 바인딩한다 — maskChar 가 한 글자가 아니면 기동을 실패시킨다. */
    public ModerationProperties(Boolean enabled, String maskChar) {
        this.enabled = enabled == null || enabled;
        String resolved = (maskChar == null || maskChar.isEmpty()) ? "*" : maskChar;
        // 두 글자 이상이면 마스킹 결과가 원문보다 길어져 컬럼 길이 제한과 어긋난다.
        if (resolved.codePointCount(0, resolved.length()) != 1) {
            throw new IllegalArgumentException("moderation.mask-char 는 한 글자여야 합니다: " + resolved);
        }
        this.maskChar = resolved;
    }
}
