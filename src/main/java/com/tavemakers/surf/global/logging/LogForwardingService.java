package com.tavemakers.surf.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@ConditionalOnProperty(name = "logging.forwarding.enabled", havingValue = "true")
public class LogForwardingService {

    private static final Logger log = LoggerFactory.getLogger(LogForwardingService.class);

    private final HttpClient httpClient;
    private final LogForwardingProperties props;

    public LogForwardingService(LogForwardingProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
    }

    /** flush() 한 번당 한 번 호출 — 한 요청의 모든 이벤트를 하나의 배치로 전송 */
    @Async("logForwardExecutor")
    public void forward(List<String> jsonLines) {
        String body = "[" + String.join(",", jsonLines) + "]";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getUrl()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        for (int attempt = 0; attempt <= props.getMaxRetries(); attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return;
                }
                log.warn("[LogForward] 전송 실패 (status={}, attempt={}/{})",
                        response.statusCode(), attempt, props.getMaxRetries());
            } catch (Exception e) {
                log.warn("[LogForward] 전송 예외 (attempt={}/{}): {}",
                        attempt, props.getMaxRetries(), e.getMessage());
            }
        }
    }
}
