package com.tavemakers.surf.application.notification.event;

import com.tavemakers.surf.domain.notification.event.NotificationCreatedEvent;

import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.infrastructure.notification.FcmService;
import com.tavemakers.surf.domain.notification.service.NotificationRenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFcmListener {

    private final NotificationRepository notificationRepository;
    private final NotificationRenderService renderer;
    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationCreatedEvent e) {
        try {
            Notification notification = notificationRepository.findById(e.notificationId())
                    .orElse(null);
            if (notification == null) return;

            String body = renderer.renderBody(notification);
            String deeplink = renderer.renderDeeplink(notification);

            fcmService.sendToMember(
                    e.receiverId(),
                    body,
                    deeplink,
                    notification.getId()
            );
        } catch (Exception ex) {
            log.warn("FCM send failed after commit. notificationId={}", e.notificationId(), ex);
        }
    }
}