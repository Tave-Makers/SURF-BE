package com.tavemakers.surf.global.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LogEventEmitterImpl implements LogEventEmitter {

    private static final Logger log = LoggerFactory.getLogger("api-event");
    private final ObjectMapper objectMapper;
    private final LogForwardingService forwardingService;

    public LogEventEmitterImpl(
            ObjectMapper objectMapper,
            @Autowired(required = false) LogForwardingService forwardingService
    ) {
        this.objectMapper = objectMapper;
        this.forwardingService = forwardingService;
    }

    /** 성공 이벤트 적재 (요청 컨텍스트에 쌓아두고, 출력은 필터에서 flush) */
    @Override
    public void emit(String event, Map<String, Object> props, String message) {
        RequestLogContext ctx = RequestLogContext.get();

        Map<String, Object> e = new HashMap<>();
        e.put("event", event);
        e.put("event_type", "INFO");
        if (message != null && !message.isBlank())
            e.put("message", message);
        e.put("props", props != null ? props : Collections.emptyMap());

        ctx.events.add(e);
    }

    /** 실패 이벤트 적재 */
    @Override
    public void emitError(String event, Map<String, Object> props, String msg) {
        RequestLogContext ctx = RequestLogContext.get();

        Map<String, Object> e = new HashMap<>();
        e.put("event", event);
        e.put("event_type", "ERROR");
        if (msg != null) e.put("message", msg);
        e.put("props", props != null ? props : Collections.emptyMap());

        ctx.events.add(e);
    }

    /** 요청 종료 시 필터(WebLoggingFilter)에서 호출: 공통 필드와 병합하여 JSON Line 출력 및 분석 서버 전송 */
    public void flush() {
        RequestLogContext ctx = RequestLogContext.get();
        List<String> jsonLines = new ArrayList<>();

        for (Map<String, Object> e : ctx.events) {
            Map<String, Object> record = new HashMap<>();
            record.put("timestamp", Instant.now().toString());
            record.put("event", e.get("event"));
            record.put("event_type", e.get("event_type"));
            record.put("result", ctx.status >= 400 ? "fail" : "success");
            record.put("status", ctx.status);
            record.put("request_id", ctx.requestId);
            record.put("user_id", ctx.userId);
            record.put("actor_role", ctx.actorRole);
            record.put("http_method", ctx.httpMethod);
            record.put("path", ctx.path);
            record.put("duration_ms", ctx.durationMs);
            if (e.containsKey("message")) {
                record.put("message", e.get("message"));
            }
            record.put("props", e.getOrDefault("props", Collections.emptyMap()));

            try {
                String json = objectMapper.writeValueAsString(record);
                log.info(json);
                jsonLines.add(json);
            } catch (Exception ex) {
                log.warn("Failed to write log record: {}", ex.toString());
            }
        }

        if (forwardingService != null && !jsonLines.isEmpty()) {
            forwardingService.forward(jsonLines);
        }
    }
}