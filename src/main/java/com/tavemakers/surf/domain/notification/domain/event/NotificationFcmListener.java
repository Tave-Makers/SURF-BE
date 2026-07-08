package com.tavemakers.surf.domain.notification.domain.event;

import com.tavemakers.surf.domain.notification.domain.entity.Notification;
import com.tavemakers.surf.domain.notification.domain.repository.NotificationRepository;
import com.tavemakers.surf.domain.notification.infrastructure.FcmService;
import com.tavemakers.surf.domain.notification.domain.service.NotificationRenderService;
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