package com.tavemakers.surf.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.event.NotificationCreatedEvent;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 알림 영속화 담당 도메인 서비스. DTO를 알지 못하며 원시값/엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(NotificationUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCreateService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1 알림 저장만 담당
     */
    public Notification create(
            Long receiverId,
            NotificationType type,
            Map<String, Object> payload
    ) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            Notification notification = Notification.of(receiverId, type, payloadJson);
            return notificationRepository.save(notification);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    /**
     * 다수 회원에게 알림 일괄 저장 + FCM 전송 (N+1 방지)
     */
    public void createAndSendBulk(
            List<Long> receiverIds,
            NotificationType type,
            Map<String, Object> payload
    ) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            List<Notification> notifications = receiverIds.stream()
                    .map(receiverId -> Notification.of(receiverId, type, payloadJson))
                    .toList();
            List<Notification> saved = notificationRepository.saveAll(notifications);
            saved.forEach(n ->
                    eventPublisher.publishEvent(new NotificationCreatedEvent(n.getId(), n.getMemberId()))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bulk notifications", e);
        }
    }
}
