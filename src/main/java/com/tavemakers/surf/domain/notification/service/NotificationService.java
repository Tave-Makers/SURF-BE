package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.exception.NotificationNotFoundException;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 알림 도메인 로직. DTO를 알지 못하며 엔티티/원시값만 다룬다.
 * 트랜잭션 경계는 호출자(NotificationUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final LogEventEmitter logEventEmitter;

    /** 알림 읽음 처리 — 본인 소유 알림만 조회·갱신한다 */
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(NotificationNotFoundException::new);
        boolean previousIsRead = notification.isRead();

        notificationRepository.markAsRead(notificationId, memberId);

        logEventEmitter.emit("notification.read", Map.of(
                "notification_id", notificationId,
                "previous_is_read", previousIsRead,
                "current_is_read", true
        ));
    }
}
