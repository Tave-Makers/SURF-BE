package com.tavemakers.surf.domain.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.notification.entity.Notification;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationRenderService {
    private final ObjectMapper objectMapper;

    /** 알림 본문 템플릿 렌더링 */
    public String renderBody(Notification notification) {
        Map<String, Object> payload = parsePayload(notification.getPayload());
        return replaceTemplate(notification.getType().getTemplate(), payload);
    }

    /** 알림 딥링크 템플릿 렌더링 */
    public String renderDeeplink(Notification notification) {
        Map<String, Object> payload = parsePayload(notification.getPayload());
        return replaceTemplate(notification.getType().getDeeplink(), payload);
    }

    /**
     * payload에서 actorId 추출
     * 기존 알림(actorId 없음)이나 시스템 알림의 경우 null 반환
     */
    public Long extractActorId(Notification notification) {
        Map<String, Object> payload = parsePayload(notification.getPayload());
        Object actorId = payload.get("actorId");
        if (actorId == null) {
            return null;
        }
        return ((Number) actorId).longValue();
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid notification payload", e);
        }
    }

    private String replaceTemplate(String template, Map<String, Object> payload) {
        String result = template;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    String.valueOf(entry.getValue())
            );
        }
        return result;
    }
}
