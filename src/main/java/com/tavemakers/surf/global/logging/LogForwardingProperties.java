package com.tavemakers.surf.global.logging;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "logging.forwarding")
public class LogForwardingProperties {

    private final boolean enabled;
    private final String url;
    private final int timeoutMs;
    private final int maxRetries;

    public LogForwardingProperties(boolean enabled, String url, int timeoutMs, int maxRetries) {
        this.enabled = enabled;
        this.url = url;
        this.timeoutMs = timeoutMs <= 0 ? 3000 : timeoutMs;
        this.maxRetries = maxRetries < 0 ? 1 : maxRetries;
    }
}
